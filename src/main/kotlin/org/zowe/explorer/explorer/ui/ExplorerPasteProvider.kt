/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.operations.mover.MoveCopyOperation
import org.zowe.explorer.explorer.FileExplorerContentProvider
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.getMinimalCommonParents
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.utils.ui.WindowsLikeMessageDialog
import org.zowe.explorer.vfs.MFVirtualFile
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
   * @param isDragAndDrop value to indicate whether the operation is DnD
   * @param operations the operation instances that carry all the necessary information about the operations
   * @param copyPasteSupport the copy-paste support to control the custom clipboard
   * @param explorerView explorer view instance to control operation
   * @param project the project to run task in
   */
  private fun runMoveOrCopyTask(
    titlePrefix: String,
    filesToMoveTotal: Int,
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
        // this step is necessary to clean old file after force overwriting performed
        if (op.forceOverwriting) {
          val nameResolver = dataOpsManager.getNameResolver(op.source, op.destination)
          op.destination.children
            .filter { file ->
              file == nameResolver.getConflictingChild(op.source, op.destination) && !file.isDirectory
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
        computeConflictAndAskForResolution(sourceFiles, pasteDestinations, project).toMutableList()
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
        val isLocalFilesPresent = ussOrLocalFileToPdsWarnings.find { it.second.isInLocalFileSystem } != null
        val fileTypesPattern = if (isLocalFilesPresent) "Local Files" else "USS Files"
        if (!showYesNoDialog(
            "$fileTypesPattern to PDS Placing",
            "You are about to place $fileTypesPattern to PDS. All lines exceeding the record length will be truncated.",
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

      if (operationsToDownload.isNotEmpty() && (!isAllConflictsResolvedBySkip(conflictsResolutions, operationsToDownload.size) || operationsToDownload.size == 1)) {
        val filesToDownloadUpdated = operationsToDownload.map { operation -> operation.source.name }

        val startMessage = "You are going to DOWNLOAD files:"
        val finishMessage = "It may be against your company's security policy. Are you sure?"

        if (
          !showYesNoDialog(
            "Downloading Files",
            createHtmlMessageWithItemsList(startMessage, filesToDownloadUpdated, finishMessage),
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
        isDragAndDrop,
        operations,
        copyPasteSupport,
        explorerView,
        project
      )
    }
  }

  /**
   * Finds name conflicts between source and target child files.
   * @param sourceFiles Files to be copied.
   * @param pasteDestinations Files (directories) to be copy to.
   * @param project Opened project.
   * @return List of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  private fun computeConflictAndAskForResolution(
    sourceFiles: List<VirtualFile>,
    pasteDestinations: List<VirtualFile>,
    project: Project?
  ): List<ConflictResolution> {
    val result = mutableListOf<ConflictResolution>()

    val conflicts = pasteDestinations.map { destFile ->

      val conflictingSources = sourceFiles.filter { source ->
        val nameResolver = dataOpsManager.getNameResolver(source, destFile)
        nameResolver.getConflictingChild(source, destFile) != null
      }
      val foundConflicts = mutableListOf<Pair<VirtualFile, VirtualFile>>()
      if (conflictingSources.isNotEmpty()) {
        conflictingSources.forEach { foundConflict -> foundConflicts.add(Pair(destFile, foundConflict)) }
      }
      foundConflicts
    }
      .flatten()
      .toMutableList()

    // Handle conflicts with different file type (file - directory, directory - file)

    val conflictsThatCannotBeOverwritten = conflicts.filter {
      val conflictChild = it.first.findChild(it.second.name)
      (conflictChild?.isDirectory == true && !it.second.isDirectory)
          || (conflictChild?.isDirectory == false && it.second.isDirectory)
          || it.first == it.second.parent
    }
    conflicts.removeAll(conflictsThatCannotBeOverwritten)

    if (conflicts.isNotEmpty() || conflictsThatCannotBeOverwritten.isNotEmpty()) {
      val choice = WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
        project = project,
        message = "The destination already has file(s) with\nthe same name.\n" +
            "Please, select an action.",
        title = "Name conflicts in ${conflicts.size + conflictsThatCannotBeOverwritten.size} file(s)",
        options = arrayOf(
          "Skip the conflicting file(s)",
          "Replace the file(s) in the destination",
          "Decide for each file"
        ),
        defaultOptionIndex = 0,
        focusedOptionIndex = 0
      )

      when (choice) {
        0 -> {
          result.addAll(conflicts.map { ConflictResolution(it.second, it.first).apply { resolveBySkip() } })
          result.addAll(
            conflictsThatCannotBeOverwritten.map { ConflictResolution(it.second, it.first).apply { resolveBySkip() } }
          )
        }

        1 -> {
          result.addAll(conflicts.map { ConflictResolution(it.second, it.first).apply { resolveByOverwrite() } })
          result.addAll(
            conflictsThatCannotBeOverwritten.map { ConflictResolution(it.second, it.first).apply { resolveBySkip() } }
          )
          if (conflictsThatCannotBeOverwritten.isNotEmpty()) {
            val startMessage = "There are some conflicts that cannot be resolved:"
            val finishMessage = "File(s) above will be skipped."
            val conflictsToShow = conflictsThatCannotBeOverwritten.map {
              if (it.first == it.second.parent) {
                "The file '${it.second.name}' cannot overwrite itself"
              } else if (it.second.isDirectory) {
                "Directory '${it.second.name}' cannot replace file '${it.second.name}'"
              } else {
                "File '${it.second.name}' cannot replace directory '${it.second.name}'"
              }
            }
            Messages.showDialog(
              project,
              createHtmlMessageWithItemsList(startMessage, conflictsToShow, finishMessage),
              "Not Resolvable Conflicts",
              arrayOf("Ok"),
              0,
              Messages.getErrorIcon()
            )
          }
        }

        2 -> result.addAll(askUserAboutConflictResolution(conflicts, conflictsThatCannotBeOverwritten, project))
        else -> throw Exception("Selected option is not supported.")
      }
    }

    return result
  }

  private fun isAllConflictsResolvedBySkip(conflicts: List<ConflictResolution>, filesToProcessSize: Int) : Boolean {
    return conflicts.isNotEmpty() && conflicts.map { it.shouldSkip() }.filter { it }.size == conflicts.size && filesToProcessSize == conflicts.size
  }

  /**
   * Resolve conflicts one by one for case when user select option "Decide for Each".
   * @param conflicts Conflict pairs (target - source) that could be resolved using any method.
   * @param conflictsThatCannotBeOverwritten Conflict pairs (target - source) that couldn't be resolved
   *                                         using "Overwrite" option.
   * @param project Opened project.
   * @return List of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  private fun askUserAboutConflictResolution(
    conflicts: List<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>,
    project: Project?
  ): List<ConflictResolution> {
    val result = mutableListOf<ConflictResolution>()
    val allConflicts = arrayListOf<Pair<VirtualFile, VirtualFile>>().apply {
      addAll(conflicts)
      addAll(conflictsThatCannotBeOverwritten)
    }

    allConflicts.forEach { conflict ->

      val newName =
        dataOpsManager.getNameResolver(conflict.second, conflict.first).resolve(conflict.second, conflict.first)

      val newNameMessage = "If you select option \"Use new name\", the following name will be selected: <b>$newName</b>"

      if (!conflictsThatCannotBeOverwritten.contains(conflict)) {
        // Conflicts between text/binary files.
        val choice = Messages.showDialog(
          project,
          "Cannot move '${conflict.second.name}' to ${conflict.first.name}\n\n$newNameMessage",
          "Name Conflict",
          arrayOf("Skip", "Overwrite", "Use new name"),
          0,
          Messages.getWarningIcon()
        )
        val resolution = ConflictResolution(conflict.second, conflict.first)
        when (choice) {
          0 -> resolution.resolveBySkip()
          1 -> resolution.resolveByOverwrite()
          2 -> resolution.resolveByUsingNewName(newName)
          else -> throw Exception("Selected option is not supported.")
        }
        result.add(resolution)
      } else {
        // Conflicts between text/binary files and directories.
        val messageToShow = if (conflict.first == conflict.second.parent) {
          "The file '${conflict.second.name}' cannot replace itself"
        } else if (conflict.second.isDirectory) {
          "Directory '${conflict.second.name}' cannot replace file '${conflict.second.name}'"
        } else {
          "File '${conflict.second.name}' cannot replace directory '${conflict.second.name}'"
        }
        val choice = Messages.showDialog(
          project,
          "$messageToShow\n\n$newNameMessage",
          "Name Conflict",
          arrayOf("Skip", "Use new name"),
          0,
          Messages.getWarningIcon()
        )
        val resolution = ConflictResolution(conflict.second, conflict.first)
        when (choice) {
          0 -> resolution.resolveBySkip()
          1 -> resolution.resolveByUsingNewName(newName)
          else -> throw Exception("Selected option is not supported.")
        }
        result.add(resolution)
      }
    }

    return result
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

  /**
   * Creates an HTML message from an items list
   * Message structure:
   *   startMessage
   *     Next, a string is constructed from the items list.
   *     Elements are added to the string until its length does not exceed the limit.
   *     If not all elements were added to the string, then "and more..." is added to the end of the string
   *   finishMessage.
   * @param startMessage beginning of the message
   * @param items list of items to display
   * @param finishMessage end of message
   * @param limit the maximum allowed length for a converted list of elements.
   * @return created HTML message
   */
  private fun createHtmlMessageWithItemsList(
    startMessage: String, items: List<String>, finishMessage: String, limit: Int = 130
  ): String {
    val pTag = "<p style=\"margin-left: 10px\">"
    val itemsMerged = items.joinToString(", ")
    val result = if (itemsMerged.length > limit)
      itemsMerged.substring(0, limit - 3).plus("...</p>${pTag}and more...")
    else
      itemsMerged
    val itemsString = pTag.plus(result).plus("</p>")
    return "<html><span>$startMessage\n</span>\n$itemsString\n<span>$finishMessage</span></html>"
  }
}
