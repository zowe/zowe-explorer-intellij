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
import com.intellij.ide.CopyPasteSupport
import com.intellij.ide.CopyProvider
import com.intellij.ide.CutProvider
import com.intellij.ide.DeleteProvider
import com.intellij.ide.PasteProvider
import com.intellij.ide.dnd.DnDManager
import com.intellij.ide.dnd.DnDSource
import com.intellij.ide.dnd.DnDTarget
import com.intellij.ide.dnd.FileCopyPasteUtil
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
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
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.mover.MoveCopyOperation
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.getMinimalCommonParents
import eu.ibagroup.formainframe.utils.getParentsChain
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.tree.TreePath
import kotlin.concurrent.withLock


/**
 * Data key for extracting current instance of FileExplorerView.
 * @see FileExplorerView
 */

const val FILE_EXPLORER_CONTEXT_MENU = "File Explorer"

// TODO: move providers somewhere?
// TODO: Yes. Try to do it. You will get a lot of fun :)
/**
 * File Explorer tree view implementation.
 * @param explorer instance of units explorer (logical representation of explorer view data).
 * @param project current project.
 * @param parentDisposable parent disposable.
 * @param contextMenu action group for context menu (with items New, Delete, Refresh and etc.).
 * @param rootNodeProvider function to get root node of the tree.
 * @param cutProviderUpdater function that will be triggered after each cut action.
 *
 * @author Viktar Mushtsin
 * @author Kiril Branavitski
 * @author Valiantsin Krus
 */
class FileExplorerView(
  explorer: Explorer<ConnectionConfig, FilesWorkingSet>,
  project: Project,
  parentDisposable: Disposable,
  contextMenu: ActionGroup,
  rootNodeProvider: (explorer: Explorer<ConnectionConfig, FilesWorkingSet>, project: Project, treeStructure: ExplorerTreeStructureBase) -> ExplorerTreeNode<ConnectionConfig, *>,
  cutProviderUpdater: (List<VirtualFile>) -> Unit
) : ExplorerTreeView<ConnectionConfig, FilesWorkingSet, FilesWorkingSetConfig>(
  explorer,
  project,
  parentDisposable,
  contextMenu,
  rootNodeProvider,
  cutProviderUpdater
) {
  override val contextMenuPlace = FILE_EXPLORER_CONTEXT_MENU
  internal val copyPasteSupport = ExplorerCopyPasteSupport(project)

  private var myDragSource: DnDSource?
  private var myDropTarget: DnDTarget?
  private var isDropTargetRegistered = false

  internal val isCut = AtomicBoolean(false)

  private val cutCopyPredicate: (NodeData<*>) -> Boolean = {
    it.attributes?.isCopyPossible == true && (!isCut.get() || it.node !is UssDirNode || !it.node.isUssMask)
  }

  /**
   * Registers drag source and drop target for explorer view
   * (it is necessary to add possibility of files drag&drop).
   */
  init {
    myDragSource = FileExplorerViewDragSource(myTree, { mySelectedNodesData }, cutCopyPredicate, copyPasteSupport)
    myDropTarget = FileExplorerViewDropTarget(myTree, explorer, copyPasteSupport)
    DnDManager.getInstance().registerSource(myDragSource!!, myTree)
    DnDManager.getInstance().registerTarget(myDropTarget, myTree)
  }

  /** Unregisters drag source and drop target. */
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

  /**
   * Explorer class that provides CopyProvider and PasteProvider.
   * @author Viktar Mushtsin
   */
  inner class ExplorerCopyPasteSupport(val project: Project?) : CopyPasteSupport {
    internal val bufferLock = ReentrantLock()

    /** buffer of files to copy */
    @Volatile
    var copyPasteBuffer = LinkedList<NodeData<*>>()

    /** buffer of files that was dragged. */
    @Volatile
    var dragAndDropCopyPasteBuffer = LinkedList<NodeData<ConnectionConfig>>()

    /** Checks if copy/cut action can be enabled and visible. */
    private fun isCopyCutEnabledAndVisible(dataContext: DataContext): Boolean {
      val nodes = dataContext.getData(ExplorerDataKeys.NODE_DATA_ARRAY)?.toList() ?: mySelectedNodesData
      return nodes.all(cutCopyPredicate)
    }

    /**
     * Removes nodes data from copy paste buffer by predicate
     * @param removePredicate predicate which will decide is node data need to be deleted.
     */
    fun removeFromBuffer(removePredicate: (NodeData<*>) -> Boolean = { true }) {
      val copyPasteBuffer = copyPasteSupport.copyPasteBuffer
      synchronized(copyPasteBuffer) {
        copyPasteBuffer.removeIf(removePredicate)
        cutProviderUpdater(copyPasteBuffer.mapNotNull { it.file })
      }
    }

    /**
     * Puts the necessary nodes data to copy paste buffer.
     * @param isCut defines if the action is cut or copy.
     * @param dataContext context that may contain NODE_DATA_ARRAY inside
     *                    (if it is then this value will be stored in buffer,
     *                    if it is not then mySelectedNodesData will be stored in buffer).
     */
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
      clearClipBoard()
    }

    /**
     * Sets empty Transferable to clipboards, which will completely clear it.
     */
    private fun clearClipBoard() {
      val emptyTransferable = object : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
          return arrayOf()
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
          return false
        }

        override fun getTransferData(flavor: DataFlavor?): Any {
          throw UnsupportedFlavorException(flavor)
        }

      }
      Toolkit.getDefaultToolkit().systemClipboard.setContents(emptyTransferable, null)

    }

    /**
     * Extracts virtual files from clipboard.
     * @return list of files in clipboard buffer.
     */
    fun getSourceFilesFromClipboard(): List<VirtualFile> {
      return CopyPasteManager.getInstance().contents?.let { tr ->
        FileCopyPasteUtil.getFileList(tr)?.mapNotNull {
          VirtualFileManager.getInstance().findFileByNioPath(it.absoluteFile.toPath())
        }
      } ?: emptyList()
    }

    /**
     * Provider for performing cut action.
     * @author Viktar Mushtsin
     */
    inner class ExplorerCutProvider : CutProvider {

      override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
      }

      /** @see ExplorerCopyPasteSupport.performCopyCut */
      override fun performCut(dataContext: DataContext) {
        performCopyCut(true, dataContext)
        //TODO("add analytics")
      }

      /** @see ExplorerCopyPasteSupport.isCopyCutEnabledAndVisible */
      override fun isCutEnabled(dataContext: DataContext): Boolean {
        return isCopyCutEnabledAndVisible(dataContext)
      }

      /** @see ExplorerCopyPasteSupport.isCopyCutEnabledAndVisible */
      override fun isCutVisible(dataContext: DataContext): Boolean {
        return isCopyCutEnabledAndVisible(dataContext)
      }
    }

    /**
     * Returns cut provider for explorer.
     * @see ExplorerCutProvider
     * @return ExplorerCutProvider instance.
     */
    override fun getCutProvider(): CutProvider {
      return ExplorerCutProvider()
    }


    /**
     * Returns copy provider for explorer.
     * @see CopyProvider
     * @return CopyProvider instance.
     */
    override fun getCopyProvider(): CopyProvider {
      return object : CopyProvider {

        override fun getActionUpdateThread(): ActionUpdateThread {
          return ActionUpdateThread.EDT
        }

        /** @see ExplorerCopyPasteSupport.performCopyCut */
        override fun performCopy(dataContext: DataContext) {
          performCopyCut(false, dataContext)
          //TODO("add analytics")
        }

        /** @see ExplorerCopyPasteSupport.isCopyCutEnabledAndVisible */
        override fun isCopyEnabled(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible(dataContext)
        }

        /** @see ExplorerCopyPasteSupport.isCopyCutEnabledAndVisible */
        override fun isCopyVisible(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible(dataContext)
        }
      }
    }

    /**
     * Registers drop target on project tree (to drag&drop files on local machine)
     * if the project tree has been initialized.
     */
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

    /**
     * Checks if paste possible in files format (not NodeData)
     * @param destinationFiles list of files to copy to.
     * @param sourceFiles list of files to copy.
     * @return true if paste is possible or false otherwise.
     */
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

    /**
     * Checks if paste possible in combined format.
     * @param destinationFiles list of files to copy to.
     * @param sourceNodesData list of nodes data of files to copy.
     * @return true if paste is possible or false otherwise.
     */
    internal fun isPastePossible(destinationFiles: List<VirtualFile>?, sourceNodesData: List<NodeData<*>>): Boolean {
      return isPastePossibleForFiles(destinationFiles, sourceNodesData.mapNotNull { it.file })
    }

    /**
     * Checks if paste possible in tree path format.
     * @param destinationPaths list of tree paths of files to copy.
     * @param sourcePaths list of tree paths of files to copy.
     * @return true if paste is possible or false otherwise.
     */
    fun isPastePossibleFromPath(destinationPaths: List<TreePath>, sourcePaths: List<TreePath?>): Boolean {
      return isPastePossible(
        destinationPaths.mapNotNull {
          makeNodeDataFromTreePath(
            explorer,
            it
          )?.let { nodeData -> if (nodeData.file is VirtualFile) nodeData.file else null }
        },
        sourcePaths.mapNotNull { makeNodeDataFromTreePath(explorer, it) }
      )
    }

    /**
     * Checks if paste enabled. Gets the sources from copy/paste buffer or drag&drop buffer.
     * @param destinationFiles list of files to copy to.
     * @return true if paste is possible and false otherise.
     */
    internal fun isPastePossibleAndEnabled(destinationFiles: List<VirtualFile>?): Boolean {
      return isPastePossible(
        destinationFiles,
        if (dragAndDropCopyPasteBuffer.size > copyPasteBuffer.size) dragAndDropCopyPasteBuffer else copyPasteBuffer
      )
    }

    /**
     * Merges source and destination files in list of pairs (from -> to)
     * @param sourceFiles list of files to copy.
     * @param destinationFiles list of files to copy to.
     * @param isCut defines is operation cut or copy.
     * @return list of pairs described above.
     */
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
        .map { destFile -> filteredSourceFiles.map { Pair(destFile, it) } }
        .flatten()
        .filter {
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

    /**
     * Returns paste provider for explorer.
     * @see ExplorerPasteProvider
     * @return ExplorerPasteProvider instance.
     */
    override fun getPasteProvider(): PasteProvider {
      return ExplorerPasteProvider()
    }
  }

  /**
   * Explorer delete provider. Provides possibility of files deletion.
   * @author Viktar Mushtsin.
   */
  private val deleteProvider = object : DeleteProvider {

    override fun getActionUpdateThread(): ActionUpdateThread {
      return ActionUpdateThread.EDT
    }

    /** Deletes files corresponding to the selected nodes data. */
    override fun deleteElement(dataContext: DataContext) {
      val selected = mySelectedNodesData
      selected.map { it.node }.filterIsInstance<FilesWorkingSetNode>()
        .forEach {
          if (
            showYesNoDialog(
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
          if (
            showYesNoDialog(
              title = "Deletion of DS Mask ${it.value.mask}",
              message = "Do you want to delete this mask from configs? Note: all data sets under it will be untouched",
              project = project,
              icon = AllIcons.General.QuestionDialog
            )
          ) {
            it.cleanCache(
              recursively = true,
              cleanFetchProviderCache = true,
              cleanBatchedQuery = true,
              sendTopic = false
            )
            it.unit.removeMask(it.value)
          }
        }
      selected.map { it.node }.filter { it is UssDirNode && it.isUssMask }
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
            node.cleanCache(
              recursively = true,
              cleanFetchProviderCache = true,
              cleanBatchedQuery = true,
              sendTopic = false
            )
            node.unit.removeUssPath(node.value)
          }
        }
      val nodeAndFilePairs = optimizeDeletion(selected)
      if (nodeAndFilePairs.isNotEmpty()) {
        val files = nodeAndFilePairs.map { it.second }.toSet().toList()
        if (showYesNoDialog(
            title = "Confirm Files Deletion",
            message = "Are you sure want to delete ${files.size} file(s)?",
            project = project,
            icon = AllIcons.General.QuestionDialog
          )
        ) {
          val filteredNodeAndFilePairs = nodeAndFilePairs.filter {
            !checkFileForSync(project, it.second, checkDependentFiles = true)
          }
          val filteredFiles = filteredNodeAndFilePairs.map { it.second }.toSet().toList()
          runModalTask(
            title = "Deletion of ${filteredFiles.size} file(s)",
            project = project,
            cancellable = true
          ) { indicator ->
            indicator.isIndeterminate = false
            ignoreVFileDeleteEvents.compareAndSet(false, true)
            filteredFiles.map { DeleteOperation(it, dataOpsManager) }
              .forEach { op ->
                indicator.text = "Deleting file ${op.file.name}"
                runCatching {
                  dataOpsManager.performOperation(op, indicator)
                }.onFailure { explorer.reportThrowable(it, project) }
                indicator.fraction = indicator.fraction + 1.0 / filteredFiles.size
              }
            filteredNodeAndFilePairs.map { it.first }.mapNotNull { it.node.parent }.distinctBy { it.path }
              .filterIsInstance<FileFetchNode<*, *, *, *, *, *>>()
              .forEach {
                it.cleanCache(
                  recursively = it is UssDirNode,
                  cleanBatchedQuery = true,
                  cleanFetchProviderCache = true,
                  sendTopic = true
                )
              }
          }
        }
      }
    }

    /**
     * Creates pairs of a node/file to optimize the removal of child element when deleting parent.
     * The function excludes working sets from the original list.
     * For other nodes - associates the corresponding parent's chain.
     * Filters out elements whose parent's chain is completely contained in the chain of another element.
     * Filters out elements that cannot be deleted
     * @param selected list of nodes to delete.
     * @return result list of pares node-file
     */
    private fun optimizeDeletion(selected: List<NodeData<ConnectionConfig>>): List<Pair<NodeData<ConnectionConfig>, MFVirtualFile>> {
      val nodeDataAndPaths = selected
        .filterNot {
          it.node is FilesWorkingSetNode || it.node is DSMaskNode || (it.node is UssDirNode && it.node.isUssMask)
        }.mapNotNull {
          Pair(it, it.file?.getParentsChain() ?: return@mapNotNull null)
        }
      val nodeDataAndPathFiltered = nodeDataAndPaths.filter { orig ->
        nodeDataAndPaths
          .filter { orig.second.size > it.second.size }
          .none { orig.second.containsAll(it.second) }
      }
      return nodeDataAndPathFiltered.map { it.first }.filter {
        val file = it.file ?: return@filter false
        explorer.componentManager.service<DataOpsManager>().isOperationSupported(
          DeleteOperation(file, dataOpsManager)
        )
      }.mapNotNull { Pair(it, it.file ?: return@mapNotNull null) }
    }

    /**
     * Checks if files corresponding to the selected nodes data can be deleted.
     * Allows you to delete only elements of one type or
     * some top-level elements (FilesWorkingSetNode/DSMaskNode/UssMask) or
     * elements in the parent-child relationship (Dataset-Member, Dir-File).
     */
    override fun canDeleteElement(dataContext: DataContext): Boolean {
      val selected = mySelectedNodesData
      val deleteOperations = selected.mapNotNull {
        DeleteOperation(it.file ?: return@mapNotNull null, it.attributes ?: return@mapNotNull null)
      }
      val nodesTypes = selected.map { it.node }
      val wsNodes = nodesTypes.filter {
        it is FilesWorkingSetNode || it is DSMaskNode || (it is UssDirNode && it.isUssMask)
      }
      val filesNodes = nodesTypes - wsNodes.toSet()

      if (nodesTypes.map { it::class.simpleName }.distinct().size == 1 &&
        (filesNodes.isEmpty()) &&
        ((nodesTypes[0] is FilesWorkingSetNode) ||
            (nodesTypes[0] is DSMaskNode) ||
            (nodesTypes[0] is UssDirNode && (nodesTypes[0] as UssDirNode).isUssMask))
      )
        return true
      val nodeAndFilePairs = optimizeDeletion(selected)
      return if (nodeAndFilePairs.isNotEmpty()) {
        val filesAttrTypes = nodeAndFilePairs.mapNotNull { it.first.attributes }.map { it::class.simpleName }.distinct()
        var checkForUss = true
        if (filesAttrTypes.any { it == "RemoteUssAttributes" })
          checkForUss = nodeAndFilePairs.map { it.first.node::class.simpleName }.distinct().size == 1
        filesAttrTypes.size == 1 && wsNodes.isEmpty() && deleteOperations.any { op ->
          dataOpsManager.isOperationSupported(op) && checkForUss
        }
      } else false
    }
  }

  /**
   * Provides data in data context. Intellij understands the context
   * from which the action was triggered and some data can be extracted
   * in this action by data keys from this context.
   * @param dataId key of the data to extract. File Explorer provides data for:
   *               1) NAVIGATABLE_ARRAY - array of selected nodes;
   *               2) COPY_PROVIDER - copy provider;
   *               3) CUT_PROVIDER - cut provider;
   *               4) PASTE_PROVIDER - paste provider;
   *               5) DELETE_ELEMENT_PROVIDER - delete provider;
   *               6) FILE_EXPLORER_VIEW - current instance of the FileExplorerView.
   * @return data corresponding to specified dataId or null if no data linked with passed dataId.
   */
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
      EXPLORER_VIEW.`is`(dataId) -> this
      else -> null
    }
  }

}

/**
 * Class containing together node, corresponding file and its attributes.
 * @author Viktar Mushtsin.
 */
data class NodeData<Connection : ConnectionConfigBase>(
  val node: ExplorerTreeNode<Connection, *>,
  val file: MFVirtualFile?,
  val attributes: FileAttributes?
)

/** Type alias for fetch node with any possible generic types. */
typealias FetchNode = FileFetchNode<*, *, *, *, *, *>
