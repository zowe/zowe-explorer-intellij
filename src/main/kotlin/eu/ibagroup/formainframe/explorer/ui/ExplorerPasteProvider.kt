package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.operations.MoveCopyOperation
import eu.ibagroup.formainframe.explorer.FileExplorerContentProvider
import eu.ibagroup.formainframe.utils.getMinimalCommonParents
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import kotlin.concurrent.withLock

object ExplorerDataKeys {
  val NODE_DATA_ARRAY = DataKey.create<Array<NodeData>>("NodeDataArrayKey")
}

class ExplorerPasteProvider: PasteProvider {
  private val dataOpsManager = service<DataOpsManager>()
  private val pastePredicate: (NodeData) -> Boolean = {
    it.attributes?.isPastePossible ?: true
  }
  override fun performPaste(dataContext: DataContext) {
    val isDragAndDrop = dataContext.getData(IS_DRAG_AND_DROP_KEY) ?: false

    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val explorerView = FileExplorerContentProvider.getInstance().getExplorerView(project)
    val copyPasteSupport = explorerView?.copyPasteSupport ?: return
    val selectedNodesData = explorerView.mySelectedNodesData
    val pasteDestinationsNodesData = selectedNodesData
      .filter(pastePredicate)
    val pasteDestinationFiles = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()
      ?: pasteDestinationsNodesData.mapNotNull { it.file }

    copyPasteSupport.bufferLock.withLock {
      val sourceFilesRaw = copyPasteSupport.copyPasteBuffer
        .mapNotNull { it.file }
        .plus(dataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) ?: emptyList())
        .plus(copyPasteSupport.getSourceFilesFromClipboard())

      val skipDestinationSourceList = mutableListOf<Pair<VirtualFile, VirtualFile>>()
      val overwriteDestinationSourceList = mutableListOf<Pair<VirtualFile, VirtualFile>>()

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

        showYesNoDialog(
          title = "$dialogTitlePrefix of ${sourceFiles.size} file(s)",
          message = "Do you want to $dialogActionMessage these files?",
          project = project
        ).let {
          if (!it) {
            copyPasteSupport.removeFromBuffer { nodeData ->
              nodeData.file?.let { fileNotNull -> sourceFiles.contains(fileNotNull) } ?: false
            }
            return@withLock
          }
        }
      }

      val conflicts = pasteDestinations
        .mapNotNull { destFile ->
          destFile.children
            ?.mapNotNull conflicts@{ destChild ->
              Pair(destFile, sourceFiles.find { source ->
                val sourceAttributes = dataOpsManager.tryToGetAttributes(source)
                val destAttributes = dataOpsManager.tryToGetAttributes(destChild)
                if (
                  destAttributes is RemoteMemberAttributes &&
                  (sourceAttributes is RemoteUssAttributes || source is VirtualFileImpl)
                ) {
                  val memberName = source.name.filter { it.isLetterOrDigit() }.take(8).toUpperCase()
                  if (memberName.isNotEmpty()) memberName == destChild.name else "EMPTY" == destChild.name
                } else if (
                  destAttributes is RemoteMemberAttributes &&
                  sourceAttributes is RemoteDatasetAttributes
                ) {
                  sourceAttributes.name.split(".").last() == destChild.name
                } else {
                  source.name == destChild.name
                }
              } ?: return@conflicts null)
            }
        }.flatten()

      if (conflicts.isNotEmpty()) {
        val choice = Messages.showDialog(
          project,
          "Please, select",
          "Name conflicts in ${conflicts.size} file(s)",
          arrayOf(
            //"Decide for Each",
            "Skip for All",
            "Overwrite for All",
          ),
          0,
          AllIcons.General.QuestionDialog,
          null
        )

        when (choice) {
          0 -> skipDestinationSourceList.addAll(conflicts)
          1 -> overwriteDestinationSourceList.addAll(conflicts)
          else -> return
        }
      }

      val ussToPdsWarnings = pasteDestinations.mapNotNull { destFile ->
        val destAttributes = dataOpsManager.tryToGetAttributes(destFile)
        if (destAttributes !is RemoteDatasetAttributes) null
        else {
          val sourceUssAttributes = sourceFiles.filter { sourceFile ->
            val sourceAttributes = dataOpsManager.tryToGetAttributes(sourceFile)
            sourceAttributes is RemoteUssAttributes || sourceFile is VirtualFileImpl
          }
          sourceUssAttributes.map { Pair(destFile, it) }.ifEmpty { null }
        }
      }.flatten()

      if (ussToPdsWarnings.isNotEmpty() &&
        !showYesNoDialog(
          "USS File To PDS Placing",
          "You are about to place USS file to PDS. All lines exceeding the record length will be truncated.",
          null,
          "Ok",
          "Skip This Files",
          AllIcons.General.WarningDialog
        )
      ) {
        skipDestinationSourceList.addAll(ussToPdsWarnings)
      }

      val operations = pasteDestinations.map { destFile ->
        sourceFiles.mapNotNull { sourceFile ->
          if (skipDestinationSourceList.contains(Pair(destFile, sourceFile))) {
            if (isDragAndDrop) {
              copyPasteSupport.removeFromBuffer { it.file == sourceFile }
            }
            return@mapNotNull null
          }
          MoveCopyOperation(
            source = sourceFile,
            destination = destFile,
            isMove = explorerView.isCut.get(),
            forceOverwriting = overwriteDestinationSourceList.contains(Pair(destFile, sourceFile)),
            newName = null,
            dataOpsManager,
            explorerView.explorer
          )
        }
      }.flatten()

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
      runModalTask(
        title = "$titlePrefix $filesToMoveTotal file(s)",
        project = project,
        cancellable = true
      ) {
        it.isIndeterminate = false
        operations.forEach { op ->
          op.sourceAttributes?.let { attr ->
            service<AnalyticsService>().trackAnalyticsEvent(
              FileEvent(
                attr,
                if (op.isMove) FileAction.MOVE else FileAction.COPY
              )
            )
          }
          it.text = "${op.source.name} to ${op.destination.name}"
          runCatching {
            dataOpsManager.performOperation(
              operation = op,
              progressIndicator = it
            )
          }.onSuccess {
            if (explorerView.isCut.get()) {
              copyPasteSupport.removeFromBuffer { it.file == op.source }
            }
          }.onFailure {
            explorerView.explorer.reportThrowable(it, project)
            if (isDragAndDrop) {
              copyPasteSupport.removeFromBuffer { it.file == op.source && operations.minus(op)
                .none { operation -> operation.source == op.source } }
            }
          }
          it.fraction = it.fraction + 1.0 / filesToMoveTotal
        }
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
          .map { explorerView.myFsTreeStructure.findByVirtualFile(it) }
          .flatten()
          .distinct()
        val nodesToRefresh = if (explorerView.isCut.get()) {
          val sourceNodesToRefresh = sourceFilesToRefresh
            .map { file -> explorerView.myFsTreeStructure.findByVirtualFile(file).map { it.parent } }
            .flatten()
            .filterNotNull()
            .distinct()
          destinationNodes.plus(sourceNodesToRefresh)
        } else {
          destinationNodes
        }

        nodesToRefresh.forEach { node ->
          // node.cleanCacheIfPossible()
          explorerView.myFsTreeStructure.findByPredicate { foundNode ->
            if (foundNode is FetchNode && node is FetchNode) {
              foundNode.query == node.query
            } else false
          }.onEach { foundNode ->
            foundNode.cleanCacheIfPossible()
          }.onEach { foundNode ->
            explorerView.myStructure.invalidate(foundNode, true)
          }

        }

      }
    }
  }

  internal fun isPastePossibleAndEnabled (dataContext: DataContext): Boolean {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return false
    val destinationFiles = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()
    val explorerView = FileExplorerContentProvider.getInstance().getExplorerView(project) ?: return false
    return explorerView.copyPasteSupport.isPastePossibleAndEnabled(destinationFiles)
  }

  override fun isPastePossible(dataContext: DataContext): Boolean {
    return isPastePossibleAndEnabled(dataContext)
  }

  override fun isPasteEnabled(dataContext: DataContext): Boolean {
    return isPastePossibleAndEnabled(dataContext)
  }
}
