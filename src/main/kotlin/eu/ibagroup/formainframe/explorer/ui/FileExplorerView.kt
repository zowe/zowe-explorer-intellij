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
import com.intellij.ide.*
import com.intellij.ide.dnd.DnDManager
import com.intellij.ide.dnd.DnDSource
import com.intellij.ide.dnd.DnDTarget
import com.intellij.ide.dnd.FileCopyPasteUtil
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.common.ui.makeNodeDataFromTreePath
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.mover.MoveCopyOperation
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.getMinimalCommonParents
import eu.ibagroup.formainframe.utils.getParentsChain
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.tree.TreePath
import kotlin.concurrent.withLock

const val FILE_EXPLORER_CONTEXT_MENU = "File Explorer"
val FILE_EXPLORER_VIEW = DataKey.create<FileExplorerView>("fileExplorerView")

// TODO: move providers somewhere?
// TODO: doc Valiantsin
/** File Explorer tree view implementation */
class FileExplorerView(
  explorer: Explorer<FilesWorkingSet>,
  project: Project,
  parentDisposable: Disposable,
  contextMenu: ActionGroup,
  rootNodeProvider: (explorer: Explorer<FilesWorkingSet>, project: Project, treeStructure: ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
  cutProviderUpdater: (List<VirtualFile>) -> Unit
) : ExplorerTreeView<FilesWorkingSet, FilesWorkingSetConfig>(
  explorer,
  project,
  parentDisposable,
  contextMenu,
  rootNodeProvider,
  cutProviderUpdater
) {
  internal val copyPasteSupport = ExplorerCopyPasteSupport(project)

  private var myDragSource: DnDSource?
  private var myDropTarget: DnDTarget?
  private var isDropTargetRegistered = false

  internal val isCut = AtomicBoolean(true)

  private val cutCopyPredicate: (NodeData) -> Boolean = {
    it.attributes?.isCopyPossible == true && (!isCut.get() || it.node !is UssDirNode || !it.node.isConfigUssPath)
  }

  init {
    myDragSource = FileExplorerViewDragSource(myTree, { mySelectedNodesData }, cutCopyPredicate, copyPasteSupport)
    myDropTarget = FileExplorerViewDropTarget(myTree, explorer, copyPasteSupport)
    DnDManager.getInstance().registerSource(myDragSource!!, myTree)
    DnDManager.getInstance().registerTarget(myDropTarget, myTree)
  }

  override fun dispose() {
    if (myDragSource != null) {
      DnDManager.getInstance().unregisterSource(myDragSource!!, myTree)
      myDragSource = null
    }
    if (myDropTarget != null) {
      DnDManager.getInstance().unregisterTarget(myDropTarget, myTree)
      myDropTarget = null
    }
  }

  inner class ExplorerCopyPasteSupport(val project: Project?) : CopyPasteSupport {
    internal val bufferLock = ReentrantLock()

    @Volatile
    var copyPasteBuffer = LinkedList<NodeData>()

    @Volatile
    var dragAndDropCopyPasteBuffer = LinkedList<NodeData>()

    private fun isCopyCutEnabledAndVisible(dataContext: DataContext): Boolean {
      val nodes = dataContext.getData(ExplorerDataKeys.NODE_DATA_ARRAY)?.toList() ?: mySelectedNodesData
      return nodes.all(cutCopyPredicate)
    }

    fun removeFromBuffer(removePredicate: (NodeData) -> Boolean = { true }) {
      val copyPasteBuffer = copyPasteSupport.copyPasteBuffer
      synchronized(copyPasteBuffer) {
        copyPasteBuffer.removeIf(removePredicate)
        cutProviderUpdater(copyPasteBuffer.mapNotNull { it.file })
      }
    }

    private fun performCopyCut(isCut: Boolean, dataContext: DataContext) {
      val nodes = dataContext.getData(ExplorerDataKeys.NODE_DATA_ARRAY)?.toList() ?: mySelectedNodesData
      this@FileExplorerView.isCut.set(isCut)
      bufferLock.withLock {
        val buffer = nodes.filter(cutCopyPredicate).apply {
          if (isCut) {
            mapNotNull { it.file }.also(cutProviderUpdater)
          } else {
            cutProviderUpdater(emptyList())
          }
          forEach {
            it.file?.let { file ->
              service<DataOpsManager>().tryToGetAttributes(file)?.let { attrs ->
                service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attrs, FileAction.COPY))
              }
            }
          }
        }.let { LinkedList(it) }
        copyPasteBuffer = buffer
      }
    }

    fun getSourceFilesFromClipboard(): List<VirtualFile> {
      return CopyPasteManager.getInstance().contents?.let { tr ->
        FileCopyPasteUtil.getFileList(tr)?.mapNotNull {
          VirtualFileManager.getInstance().findFileByNioPath(it.absoluteFile.toPath())
        }
      } ?: emptyList()
    }

    inner class ExplorerCutProvider : CutProvider {

      override fun performCut(dataContext: DataContext) {
        performCopyCut(true, dataContext)
        //TODO("add analytics")
      }

      override fun isCutEnabled(dataContext: DataContext): Boolean {
        return isCopyCutEnabledAndVisible(dataContext)
      }

      override fun isCutVisible(dataContext: DataContext): Boolean {
        return isCopyCutEnabledAndVisible(dataContext)
      }
    }

    override fun getCutProvider(): CutProvider {
      return ExplorerCutProvider()
    }

    override fun getCopyProvider(): CopyProvider {
      return object : CopyProvider {
        override fun performCopy(dataContext: DataContext) {
          performCopyCut(false, dataContext)
          //TODO("add analytics")
        }

        override fun isCopyEnabled(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible(dataContext)
        }

        override fun isCopyVisible(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible(dataContext)
        }
      }
    }

    internal fun registerDropTargetInProjectViewIfNeeded() {
      if (isDropTargetRegistered) {
        return
      }
      project?.let {
        val tree = ProjectView.getInstance(project).currentProjectViewPane?.tree
        tree?.let {
          DnDManager.getInstance().registerTarget(myDropTarget, tree)
          isDropTargetRegistered = true
        }
      }
    }

    internal fun isPastePossibleForFiles(
      destinationFiles: List<VirtualFile>?,
      sourceFiles: List<VirtualFile>
    ): Boolean {
      registerDropTargetInProjectViewIfNeeded()
      val destFiles = destinationFiles ?: mySelectedNodesData.mapNotNull { it.file }
      return bufferLock.withLock {
        getDestinationSourceFilePairs(
          sourceFiles = sourceFiles.plus(copyPasteSupport.getSourceFilesFromClipboard()).distinct(),
          destinationFiles = destFiles,
          isCut = isCut.get()
        ).isNotEmpty()
      }
    }

    internal fun isPastePossible(destinationFiles: List<VirtualFile>?, sourceNodesData: List<NodeData>): Boolean {
      return isPastePossibleForFiles(destinationFiles, sourceNodesData.mapNotNull { it.file })
    }

    fun isPastePossibleFromPath(destinationPaths: List<TreePath>, sourcePaths: List<TreePath?>): Boolean {
      return isPastePossible(
        destinationPaths.mapNotNull {
          makeNodeDataFromTreePath(
            explorer,
            it
          ).let { nodeData -> if (nodeData.file is VirtualFile) nodeData.file else null }
        },
        sourcePaths.map { makeNodeDataFromTreePath(explorer, it) }
      )
    }


    internal fun isPastePossibleAndEnabled(destinationFiles: List<VirtualFile>?): Boolean {
      return isPastePossible(
        destinationFiles,
        if (dragAndDropCopyPasteBuffer.size > copyPasteBuffer.size) dragAndDropCopyPasteBuffer else copyPasteBuffer
      )
    }

    fun getDestinationSourceFilePairs(
      sourceFiles: List<VirtualFile>,
      destinationFiles: List<VirtualFile>,
      isCut: Boolean
    ): List<Pair<VirtualFile, VirtualFile>> {

      val filteredSourceFiles = if (isCut) {
        sourceFiles.getMinimalCommonParents()
      } else {
        sourceFiles
      }

      return destinationFiles
        .map { destFile ->
          filteredSourceFiles.map { Pair(destFile, it) }
        }.flatten().filter {
          dataOpsManager.isOperationSupported(
            operation = MoveCopyOperation(
              source = it.second,
              destination = it.first,
              isMove = isCut,
              forceOverwriting = false,
              newName = null,
              dataOpsManager,
              explorer
            )
          )
        }
    }

    override fun getPasteProvider(): PasteProvider {
      return ExplorerPasteProvider()
    }
  }

  private val deleteProvider = object : DeleteProvider {
    override fun deleteElement(dataContext: DataContext) {
      val selected = mySelectedNodesData
      selected.map { it.node }.filterIsInstance<FilesWorkingSetNode>()
        .forEach {
          if (showYesNoDialog(
              title = "Deletion of Working Set ${it.unit.name}",
              message = "Do you want to delete this Working Set from configs? Note: all data under it will be untouched",
              project = project,
              icon = AllIcons.General.QuestionDialog
            )
          ) {
            explorer.disposeUnit(it.unit as FilesWorkingSet)
          }
        }
      selected.map { it.node }.filterIsInstance<DSMaskNode>()
        .filter { explorer.isUnitPresented(it.unit) }
        .forEach {
          if (showYesNoDialog(
              title = "Deletion of DS Mask ${it.value.mask}",
              message = "Do you want to delete this mask from configs? Note: all data sets under it will be untouched",
              project = project,
              icon = AllIcons.General.QuestionDialog
            )
          ) {
            it.unit.removeMask(it.value)
          }
        }
      selected.map { it.node }.filter { it is UssDirNode && it.isConfigUssPath }
        .filter { explorer.isUnitPresented((it as UssDirNode).unit) }
        .forEach {
          val node = it as UssDirNode
          if (showYesNoDialog(
              title = "Deletion of Uss Path Root ${node.value.path}",
              message = "Do you want to delete this USS path root from configs? Note: all files under it will be untouched",
              project = project,
              icon = AllIcons.General.QuestionDialog
            )
          ) {
            node.unit.removeUssPath(node.value)
          }
        }
      val nodeDataAndPaths = selected
        .filterNot {
          it.node is FilesWorkingSetNode || it.node is DSMaskNode || (it.node is UssDirNode && it.node.isConfigUssPath)
        }.mapNotNull {
          Pair(it, it.file?.getParentsChain() ?: return@mapNotNull null)
        }
      val nodeDataAndPathFiltered = nodeDataAndPaths.filter { orig ->
        nodeDataAndPaths
          .filter { orig.second.size > it.second.size }
          .none { orig.second.containsAll(it.second) }
      }
      val nodeAndFilePairs = nodeDataAndPathFiltered.map { it.first }.filter {
        val file = it.file ?: return@filter false
        explorer.componentManager.service<DataOpsManager>().isOperationSupported(
          DeleteOperation(file, dataOpsManager)
        )
      }.mapNotNull { Pair(it, it.file ?: return@mapNotNull null) }
      if (nodeAndFilePairs.isNotEmpty()) {
        val files = nodeAndFilePairs.map { it.second }.toSet().toList()
        if (showYesNoDialog(
            title = "Confirm Files Deletion",
            message = "Are you sure want to delete ${files.size} file(s)?",
            project = project,
            icon = AllIcons.General.QuestionDialog
          )
        ) {
          runModalTask(
            title = "Deletion of ${files.size} file(s)",
            project = project,
            cancellable = true
          ) {
            it.isIndeterminate = false
            ignoreVFileDeleteEvents.compareAndSet(false, true)
            files.map { DeleteOperation(it, dataOpsManager) }
              .forEach { op ->
                it.text = "Deleting file ${op.file.name}"
                runCatching {
                  dataOpsManager.performOperation(op, it)
                }.onFailure { explorer.reportThrowable(it, project) }
                it.fraction = it.fraction + 1.0 / files.size
              }
            nodeAndFilePairs.map { it.first }.mapNotNull { it.node.parent }
              .filterIsInstance<FileFetchNode<*, *, *, *, *>>()
              .forEach { it.cleanCache(true) }
          }
        }
      }
    }

    override fun canDeleteElement(dataContext: DataContext): Boolean {
      val selected = mySelectedNodesData
      val deleteOperations = selected.mapNotNull {
        DeleteOperation(it.file ?: return@mapNotNull null, it.attributes ?: return@mapNotNull null)
      }
      return selected.any {
        it.node is FilesWorkingSetNode
                || it.node is DSMaskNode
                || (it.node is UssDirNode && it.node.isConfigUssPath)
                || deleteOperations.any { op -> dataOpsManager.isOperationSupported(op) }
      }
    }
  }

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId) -> mySelectedNodesData.filter {
        val file = it.file
        if (file != null) {
          val attributes = service<DataOpsManager>().tryToGetAttributes(file) as? RemoteDatasetAttributes
          val isMigrated = attributes?.isMigrated ?: false
          !isMigrated
        }
        true
      }.map { it.node }.toTypedArray()
      PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> copyPasteSupport.copyProvider
      PlatformDataKeys.CUT_PROVIDER.`is`(dataId) -> copyPasteSupport.cutProvider
      PlatformDataKeys.PASTE_PROVIDER.`is`(dataId) -> copyPasteSupport.pasteProvider
      PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId) -> deleteProvider
      FILE_EXPLORER_VIEW.`is`(dataId) -> this
      else -> null
    }
  }

}

data class NodeData(
  val node: ExplorerTreeNode<*>,
  val file: MFVirtualFile?,
  val attributes: FileAttributes?
)

typealias FetchNode = FileFetchNode<*, *, *, *, *>

