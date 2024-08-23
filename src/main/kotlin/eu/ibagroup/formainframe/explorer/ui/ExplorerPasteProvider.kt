/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.common.ui.cleanInvalidateOnExpand
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.operations.mover.MoveCopyOperation
import eu.ibagroup.formainframe.explorer.FileExplorerContentProvider
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.getMinimalCommonParents
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DatasetOrganization
import kotlin.concurrent.withLock

object ExplorerDataKeys {
  val NODE_DATA_ARRAY = DataKey.create<Array<NodeData<*>>>("NodeDataArrayKey")
}

/**
 * Wrapper for conflict. It contains source and destination files that have a conflict between them.
 * It is also needed to indicate how to properly resolve them. 3 options are possible: skip, overwrite, use new name.
 * @param sourceFile file to be copied.
 * @param destinationFile child of file to be copy to that have conflict with source file.
 * @author Valiantsin Krus
 */
class ConflictResolution(
  val sourceFile: VirtualFile,
  val destinationFile: VirtualFile
) {
  private var overwrite: Boolean = false
  private var skip: Boolean = false
  var newName: String? = null
    private set

  /** Indicates that conflict should be resolved by overwriting destination file. */
  fun resolveByOverwrite() {
    overwrite = true
    skip = false
    newName = null
  }

  /** Indicates that conflict should be skipped. Source file will not be copied in such case. */
  fun resolveBySkip() {
    skip = true
    overwrite = false
    newName = null
  }

  /**
   *  Indicate that source file should have new name in target.
   *  In such way we will save both target and source files.
   *  @param newName the name of the source file in the target directory after copying.
   */
  fun resolveByUsingNewName(newName: String) {
    if (newName == destinationFile.name) {
      throw IllegalArgumentException("Resolve name should be the same as destination file name: '${newName}'.")
    }
    this.newName = newName
    overwrite = false
    skip = false
  }

  /** @return true only if conflict should be skipped. */
  fun shouldSkip(): Boolean {
    return skip
  }

  /** @return true only if conflict should be solved by overwriting destination file. */
  fun shouldOverwrite(): Boolean {
    return overwrite
  }

  /** @return true only if source file should change name in target directory. */
  fun shouldUseNewName(): Boolean {
    return newName != null
  }
}

/**
 * Implementation of Intellij PasteProvider.
 * Used to perform paste of files (in MF File Explorer and in Local File Explorer).
 * @author Valiantsin Krus
 * @author Viktar Mushtsin
 */
class ExplorerPasteProvider : PasteProvider {
  private val dataOpsManager = service<DataOpsManager>()
  private val pastePredicate: (NodeData<*>) -> Boolean = {
    it.attributes?.isPastePossible ?: true
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Get nodes to refresh. Normally it would be some parent nodes that are changed during the copy/move operation.
   * E.g. for copy operation it will be parent node of the destination. For move operation it will be both source and destination nodes
   * @param operations operations list to find nodes by
   * @param explorerView the explorer view to get filesystem tree structure and the operation type
   * @return nodes to refresh
   */
  private fun getNodesToRefresh(
    operations: List<MoveCopyOperation>,
    explorerView: FileExplorerView
  ): List<ExplorerTreeNode<*, *>> {
    fun List<MoveCopyOperation>.collectByFile(
      fileChooser: (MoveCopyOperation) -> VirtualFile
    ): List<VirtualFile> {
      return map(fileChooser)
        .distinct()
        .getMinimalCommonParents()
        .toList()
    }

    val destinationFilesToRefresh = operations.collectByFile { it.destination }
    val sourceFilesToRefresh = if (explorerView.isCut.get()) {
      operations.collectByFile { it.source }
    } else {
      emptyList()
    }
    val destinationNodes = destinationFilesToRefresh
      .map { file -> explorerView.myFsTreeStructure.findByVirtualFile(file).reversed() }
      .flatten()
      .distinct()
    return if (explorerView.isCut.get()) {
      val sourceNodesToRefresh = sourceFilesToRefresh
        .map { file -> explorerView.myFsTreeStructure.findByVirtualFile(file).reversed().map { it.parent } }
        .flatten()
        .filterNotNull()
        .distinct()
      sourceNodesToRefresh.plus(destinationNodes)
    } else {
      destinationNodes
    }
  }

  /**
   * Refresh provided nodes
   * @param nodesToRefresh the nodes to refresh
   * @param explorerView the explorer view to clean "invalidate on expand" for nodes
   */
  private fun refreshNodes(nodesToRefresh: List<ExplorerTreeNode<*, *>>, explorerView: FileExplorerView) {
    nodesToRefresh
      .forEach { node ->
        val parentNode = node.castOrNull<FileFetchNode<*, *, *, *, *, *>>() ?: return
        parentNode.query ?: return
        cleanInvalidateOnExpand(parentNode, explorerView)
        parentNode.cleanCache(cleanBatchedQuery = true).let { explorerView.myStructure.invalidate(parentNode, true) }
      }
  }

  /**
   * Run move or copy task. Cleans cache on subtrees after the operation is completed
   * @param titlePrefix the operation title
   * @param filesToMoveTotal count of files to perform operation on
   * @param sourceFiles list of source files to operation
   * @param isDragAndDrop value to indicate whether the operation is DnD
   * @param operations the operation instances that carry all the necessary information about the operations
   * @param copyPasteSupport the copy-paste support to control the custom clipboard
   * @param explorerView explorer view instance to control operation
   * @param project the project to run task in
   */
  private fun runMoveOrCopyTask(
    titlePrefix: String,
    filesToMoveTotal: Int,
    sourceFiles: List<VirtualFile>,
    isDragAndDrop: Boolean,
    operations: List<MoveCopyOperation>,
    copyPasteSupport: FileExplorerView.ExplorerCopyPasteSupport,
    explorerView: FileExplorerView,
    project: Project
  ) {
    runModalTask(
      title = "$titlePrefix $filesToMoveTotal file(s)",
      project = project,
      cancellable = true
    ) {
      it.isIndeterminate = false
      operations.forEach { op ->
        op.sourceAttributes?.let { attr ->
          service<AnalyticsService>()
            .trackAnalyticsEvent(
              FileEvent(
                attr,
                if (op.isMove) FileAction.MOVE else FileAction.COPY
              )
            )
        }
        // this step is necessary to clean old file after force overwriting performed
        if (op.forceOverwriting) {
          val nameResolver = dataOpsManager.getNameResolver(op.source, op.destination)
          op.destination.children
            .filter { file ->
              file == nameResolver.getConflictingChild(op.source, sourceFiles, op.destination) && !file.isDirectory
            }
            .apply {
              runWriteActionInEdtAndWait {
                forEach { file -> file.delete(this) }
              }
            }
        }
        explorerView.ignoreVFSChangeEvents.compareAndSet(false, true)
        it.text = "${op.source.name} to ${op.destination.name}"
        runCatching {
          dataOpsManager.performOperation(
            operation = op,
            progressIndicator = it
          )
        }
          .onSuccess {
            if (explorerView.isCut.get()) {
              copyPasteSupport.removeFromBuffer { node -> node.file == op.source }
            }
          }
          .onFailure { throwable ->
            explorerView.explorer.reportThrowable(throwable, project)
            if (isDragAndDrop) {
              copyPasteSupport.removeFromBuffer { node ->
                node.file == op.source && operations.minus(op).none { operation -> operation.source == op.source }
              }
            }
            else if(explorerView.isCut.get()) {
              copyPasteSupport.removeFromBuffer { node -> node.file == op.source }
            }
          }
        it.fraction = it.fraction + 1.0 / filesToMoveTotal
      }

      val nodesToRefresh = getNodesToRefresh(operations, explorerView)

      refreshNodes(nodesToRefresh, explorerView)
    }
  }

  /**
   * Performs paste from ForMainframe buffer and from clipboard buffer of local file system.
   * ForMainframe buffer is just a list of NodeData to copy or cut.
   * @param dataContext Current context. This method can use the list of context keys below:
   *                    1) PROJECT {Project} - opened project.
   *                    2) IS_DRAG_AND_DROP_KEY {boolean} - identifies if the copying performed from drag&drop.
   *                    3) VIRTUAL_FILE_ARRAY {list} - list of destination files (can be empty if all destinations
   *                                                   are MF files, in this case destinations fetched from NodeData)
   *                    4) DRAGGED_FROM_PROJECT_FILES_ARRAY {list} - list of files that was dragged from project files
   *                                                                 tree (empty if operation is not drag&drop)
   */
  override fun performPaste(dataContext: DataContext) {
    val isDragAndDrop = dataContext.getData(IS_DRAG_AND_DROP_KEY) ?: false

    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val explorerView = FileExplorerContentProvider.getInstance().getExplorerView(project).castOrNull<FileExplorerView>()
    val copyPasteSupport = explorerView?.copyPasteSupport ?: return
    val selectedNodesData = explorerView.mySelectedNodesData
    val pasteDestinationsNodesData = selectedNodesData
      .filter(pastePredicate)
    val pasteDestinationFiles = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()
      ?: pasteDestinationsNodesData.mapNotNull { it.file }

    val draggedBuffer = dataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) ?: emptyList()
    val clipboardBuffer = copyPasteSupport.getSourceFilesFromClipboard()
    copyPasteSupport.bufferLock.withLock {
      val sourceFilesRaw = clipboardBuffer.ifEmpty {
        copyPasteSupport.copyPasteBuffer
          .mapNotNull { it.file }
          .plus(draggedBuffer)
      }

      val destinationSourceFilePairs = copyPasteSupport.getDestinationSourceFilePairs(
        sourceFiles = sourceFilesRaw,
        destinationFiles = pasteDestinationFiles,
        isCut = explorerView.isCut.get()
      )

      val pasteDestinations = destinationSourceFilePairs.map { it.first }.toSet().toList()
      val sourceFiles = destinationSourceFilePairs.map { it.second }.toSet().toList()

      if (explorerView.isCut.get()) {
        val hasRemoteFilesInDestinations = pasteDestinations.any { it is MFVirtualFile }
        val hasLocalFilesInSources = sourceFiles.any { it !is MFVirtualFile }

        val dialogTitlePrefix = if (!hasLocalFilesInSources && hasRemoteFilesInDestinations) "Moving"
        else if (hasLocalFilesInSources && hasRemoteFilesInDestinations) "Uploading"
        else "Downloading"

        val dialogActionMessage = if (!hasLocalFilesInSources && hasRemoteFilesInDestinations) "move"
        else if (hasLocalFilesInSources && hasRemoteFilesInDestinations) "upload"
        else "download"

        val dialogMessagePrefix = if (dialogTitlePrefix == "Moving") ""
        else "$dialogTitlePrefix files can violate security rules of customer who owns this system.\n\n"

        showYesNoDialog(
          title = "$dialogTitlePrefix of ${sourceFiles.size} file(s)",
          message = "${dialogMessagePrefix}Do you want to $dialogActionMessage these files?",
          project = project,
          icon = if (dialogTitlePrefix == "Moving") null else AllIcons.General.WarningDialog
        ).let { proceed ->
          if (!proceed) {
            copyPasteSupport.removeFromBuffer { nodeData ->
              nodeData.file?.let { fileNotNull -> sourceFiles.contains(fileNotNull) } ?: false
            }
            return@withLock
          }
        }
      }

      // conflicts start
      val conflictsResolutions = runCatching {
        if (dataOpsManager.tryToGetAttributes(pasteDestinations[0]) is RemoteDatasetAttributes)
          RemoteDatasetComputeConflicts(dataOpsManager, sourceFiles, pasteDestinations, project).computeConflictAndAskForResolution().toMutableList()
        else
          CommonComputeConflicts(dataOpsManager, sourceFiles, pasteDestinations, project).computeConflictAndAskForResolution().toMutableList()
    }.getOrNull() ?: return

      // conflicts end
      // specific configs resolution
      val ussOrLocalFileToPdsWarnings = pasteDestinations
        .mapNotNull { destFile ->
          val destAttributes = dataOpsManager.tryToGetAttributes(destFile)
          if (destAttributes !is RemoteDatasetAttributes) null
          else {
            val sourceUssAttributes = sourceFiles
              .filter { sourceFile ->
                val sourceAttributes = dataOpsManager.tryToGetAttributes(sourceFile)
                sourceAttributes is RemoteUssAttributes || sourceFile.isInLocalFileSystem
              }
            sourceUssAttributes.map { Pair(destFile, it) }.ifEmpty { null }
          }
        }
        .flatten()

      if (ussOrLocalFileToPdsWarnings.isNotEmpty() && !isAllConflictsResolvedBySkip(conflictsResolutions, ussOrLocalFileToPdsWarnings.size )) {
        val foundDestAttributes = ussOrLocalFileToPdsWarnings
          .mapNotNull { it.first.let { file -> dataOpsManager.tryToGetAttributes(file) } }
          .map { it as RemoteDatasetAttributes }
          .mapNotNull { it.datasetInfo.datasetOrganization }
          .distinct()
        val destFilesPattern = foundDestAttributes.let { if (it.contains(DatasetOrganization.PO) && it.contains(DatasetOrganization.POE)) "PDS and PDS/E" else if (it.contains(DatasetOrganization.PO)) "PDS" else "PDS/E" }
        val isLocalFilesPresentInSources = ussOrLocalFileToPdsWarnings.find { it.second.isInLocalFileSystem } != null
        val sourceFileTypesPattern = if (isLocalFilesPresentInSources) "Local Files" else "USS Files"
        if (!showYesNoDialog(
            "$sourceFileTypesPattern to $destFilesPattern Placing",
            "You are about to place $sourceFileTypesPattern to $destFilesPattern. All lines exceeding the record length will be truncated.",
            null,
            "Ok",
            "Skip This Files",
            AllIcons.General.WarningDialog
          )
        ) {
          conflictsResolutions.apply {
            clear()
            addAll(ussOrLocalFileToPdsWarnings.map { ConflictResolution(it.second, it.first).apply { resolveBySkip() } })
          }
        }
      }
      // specific conflicts resolution end

      val operations = pasteDestinations
        .map { destFile ->
          sourceFiles.mapNotNull { sourceFile ->
            val conflictResolution = conflictsResolutions
              .find { it.sourceFile == sourceFile && it.destinationFile == destFile }
            if (conflictResolution?.shouldSkip() == true) {
              if (isDragAndDrop) {
                copyPasteSupport.removeFromBuffer { it.file == sourceFile }
              }
              return@mapNotNull null
            }
            MoveCopyOperation(
              source = sourceFile,
              destination = destFile,
              isMove = explorerView.isCut.get(),
              forceOverwriting = conflictResolution?.shouldOverwrite() == true,
              newName = conflictResolution?.newName,
              dataOpsManager,
              explorerView.explorer
            )
          }
        }
        .flatten()

      val operationsToDownload = operations
        .filter { operation -> operation.destination !is MFVirtualFile }

      if (operationsToDownload.isNotEmpty()) {
        val filesToDownloadUpdated = operationsToDownload.map { operation -> operation.source.name }

        val startMessage = "You are going to DOWNLOAD files:"
        val finishMessage = "It may be against your company's security policy. Are you sure?"

        if (
          !showYesNoDialog(
            "Downloading Files",
            ComputeConflicts.createHtmlMessageWithItemsList(startMessage, filesToDownloadUpdated, finishMessage),
            null,
            "Yes",
            "No",
            AllIcons.General.WarningDialog
          )
        ) {
          return
        }
      }

      val filesToMoveTotal = operations.size
      val hasLocalFilesInOperationsSources = operations.any { it.source !is MFVirtualFile }
      val hasRemoteFilesInOperationsDestinations = operations.any { it.destination is MFVirtualFile }
      val titlePrefix = if (explorerView.isCut.get()) {
        if (!hasLocalFilesInOperationsSources && hasRemoteFilesInOperationsDestinations) {
          "Moving"
        } else if (!hasLocalFilesInOperationsSources) {
          "Downloading"
        } else {
          "Uploading"
        }
      } else {
        "Copying"
      }

      runMoveOrCopyTask(
        titlePrefix,
        filesToMoveTotal,
        sourceFiles,
        isDragAndDrop,
        operations,
        copyPasteSupport,
        explorerView,
        project
      )
    }
  }

  private fun isAllConflictsResolvedBySkip(conflicts: List<ConflictResolution>, filesToProcessSize: Int) : Boolean {
    return conflicts.isNotEmpty() && conflicts.map { it.shouldSkip() }.filter { it }.size == conflicts.size && filesToProcessSize == conflicts.size
  }

  /**
   * Checks if the paste possible and enabled.
   * @param dataContext Current context. This method can use the list of context keys below:
   *                    1) PROJECT {Project} - opened project.
   *                    2) VIRTUAL_FILE_ARRAY {list} - list of destination files (can be empty if all destinations
   *                                                   are MF files, in this case destinations fetched from NodeData)
   */
  internal fun isPastePossibleAndEnabled(dataContext: DataContext): Boolean {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return false
    val destinationFiles = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()
    val explorerView =
      FileExplorerContentProvider.getInstance().getExplorerView(project).castOrNull<FileExplorerView>() ?: return false
    return explorerView.copyPasteSupport.isPastePossibleAndEnabled(destinationFiles)
  }

  /**
   * Does the same as isPastePossibleAndEnabled.
   * @see ExplorerPasteProvider.isPastePossibleAndEnabled
   */
  override fun isPastePossible(dataContext: DataContext): Boolean {
    return isPastePossibleAndEnabled(dataContext)
  }

  /**
   * Does the same as isPastePossibleAndEnabled.
   * @see ExplorerPasteProvider.isPastePossibleAndEnabled
   */
  override fun isPasteEnabled(dataContext: DataContext): Boolean {
    return isPastePossibleAndEnabled(dataContext)
  }
}
