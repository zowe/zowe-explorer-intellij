package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.*
import com.intellij.ide.dnd.*
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.common.ui.DoubleClickTreeMouseListener
import eu.ibagroup.formainframe.common.ui.makeNodeDataFromTreePath
import eu.ibagroup.formainframe.common.ui.promisePath
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.attributesListener
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.dataops.fetch.FileCacheListener
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.MoveCopyOperation
import eu.ibagroup.formainframe.explorer.*
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import kotlin.concurrent.withLock

val FILE_EXPLORER_VIEW = DataKey.create<GlobalFileExplorerView>("fileExplorerView")

abstract class ExplorerTreeView<U : WorkingSet<*>, UnitConfig : EntityWithUuid>
  (
  val explorer: Explorer<U>,
  project: Project,
  parentDisposable: Disposable,
  private val contextMenu: ActionGroup,
  rootNodeProvider: (explorer: Explorer<U>, project: Project, treeStructure: ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
  internal val cutProviderUpdater: (List<VirtualFile>) -> Unit
) : JBScrollPane(), DataProvider, Disposable {


  internal var mySelectedNodesData: List<NodeData> by rwLocked(listOf())
  internal val myFsTreeStructure: CommonExplorerTreeStructure<Explorer<U>>
  internal val myStructure: StructureTreeModel<CommonExplorerTreeStructure<Explorer<U>>>
  internal val myTree: Tree
  internal val myNodesToInvalidateOnExpand = hashSetOf<Any>()

  protected val dataOpsManager = explorer.componentManager.service<DataOpsManager>()
  protected val ignoreVFileDeleteEvents = AtomicBoolean(false)

  private var treeModel: AsyncTreeModel


  internal fun getNodesByQueryAndInvalidate(
    query: Query<*, *>, collapse: Boolean = false, invalidate: Boolean = true
  ): Collection<ExplorerTreeNode<*>> {
    return myFsTreeStructure.findByPredicate {
      if (it is FetchNode) {
        it.query == query
      } else false
    }.onEach { foundNode ->
      fun invalidate() = myStructure.invalidate(foundNode, true)

      fun collapseIfNeeded(tp: TreePath) {
        if (collapse) {
          treeModel.onValidThread {
            myTree.collapsePath(tp)
            synchronized(myNodesToInvalidateOnExpand) {
              val node = tp.lastPathComponent
              myNodesToInvalidateOnExpand.add(node)
            }
          }
        }
      }
      myStructure.promisePath(foundNode, myTree).onSuccess { nodePath ->
        if (myTree.isVisible(nodePath) && myTree.isCollapsed(nodePath) && mySelectedNodesData.any { it.node == nodePath }) {
          myTree.expandPath(nodePath)
        }
        if (invalidate) {
          invalidate().onSuccess { tp ->
            collapseIfNeeded(tp)
          }
        } else {
          collapseIfNeeded(nodePath)
        }
      }
    }
  }

  init {
    Disposer.register(parentDisposable, this)
    myStructure = StructureTreeModel(
      CommonExplorerTreeStructure(explorer, project, rootNodeProvider).also { myFsTreeStructure = it },
      { o1, o2 ->
        if (o1 is FilesWorkingSetNode && o2 is FilesWorkingSetNode) {
          o1.unit.name.compareTo(o2.unit.name)
        } else {
          0
        }
      },
      this
    ).also { stm ->
      treeModel = AsyncTreeModel(stm, false, this).also {
        myTree = DnDAwareTree(it).apply { isRootVisible = false }.also { t ->
          setViewportView(t)
          registerTreeListeners(t)
        }
      }
    }
    subscribe(
      componentManager = explorer.componentManager,
      topic = UNITS_CHANGED,
      handler = object : ExplorerListener {


        private fun onAddDelete(explorer: Explorer<*>) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(explorer).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }


        override fun onAdded(explorer: Explorer<*>, unit: ExplorerUnit) {
          onAddDelete(explorer)
        }

        override fun onChanged(explorer: Explorer<*>, unit: ExplorerUnit) {
          if (explorer == this@ExplorerTreeView.explorer) {
            myFsTreeStructure.findByValue(unit).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun onDeleted(explorer: Explorer<*>, unit: ExplorerUnit) {
          onAddDelete(explorer)
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = ApplicationManager.getApplication(),
      topic = VirtualFileManager.VFS_CHANGES,
      handler = object : BulkFileListener {
        override fun after(events: MutableList<out VFileEvent>) {
          events.mapNotNull {
            val nodes = myFsTreeStructure.findByVirtualFile(it.file ?: return@mapNotNull null)
            when {
              it is VFileContentChangeEvent || it is VFilePropertyChangeEvent -> {
                nodes
              }
              it is VFileDeleteEvent
                  && this@ExplorerTreeView
                .ignoreVFileDeleteEvents
                .compareAndSet(true, true) -> {
                null
              }
              else -> {
                nodes.mapNotNull { n -> n.parent }
              }
            }
          }.flatten().forEach { fileNode ->
            fileNode.cleanCacheIfPossible()
            myStructure.invalidate(fileNode, true)
          }
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = explorer.componentManager,
      topic = CutBufferListener.CUT_BUFFER_CHANGES,
      handler = CutBufferListener { previousBufferState, currentBufferState ->
        previousBufferState
          .asSequence()
          .plus(currentBufferState)
          .distinct()
          .map { it.getAncestorNodes() }
          .flatten()
          .distinct()
          .map {
            myFsTreeStructure.findByVirtualFile(it)
          }.flatten()
          .distinct()
          .forEach {
            myStructure.invalidate(it, true)
          }
      }
    )
    subscribe(
      componentManager = explorer.componentManager,
      topic = FileFetchProvider.CACHE_CHANGES,
      handler = object : FileCacheListener {

        override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> onCacheUpdated(
          query: Q,
          files: Collection<File>
        ) {
          getNodesByQueryAndInvalidate(query)
        }

        override fun <R : Any, Q : Query<R, Unit>> onCacheCleaned(query: Q) {
          getNodesByQueryAndInvalidate(query)
        }

        override fun <R : Any, Q : Query<R, Unit>> onFetchCancelled(query: Q) {
          getNodesByQueryAndInvalidate(query, collapse = true, invalidate = false)
        }

        override fun <R : Any, Q : Query<R, Unit>> onFetchFailure(query: Q, throwable: Throwable) {
          getNodesByQueryAndInvalidate(query)
          explorer.reportThrowable(throwable, project)
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = dataOpsManager.componentManager,
      topic = AttributesService.FILE_CONTENT_CHANGED,
      handler = attributesListener<FileAttributes, VirtualFile> {
        onUpdate { _, _, file ->
          val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(project))
          dataOpsManager.getContentSynchronizer(file)?.synchronizeWithRemote(syncProvider, progressIndicator = null)
        }
      }
    )
  }


  private fun registerTreeListeners(tree: DnDAwareTree) {
    val contextMenuPlace : String = when (this) {
      is GlobalFileExplorerView -> FILE_EXPLORER_CONTEXT_MENU
      is JesExplorerView -> JES_EXPLORER_CONTEXT_MENU
      else -> { "Unrecognized" }
    }

    tree.addMouseListener(object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {
        val popupActionGroup = DefaultActionGroup()
        popupActionGroup.add(
          contextMenu
        )
        val popupMenu = ActionManager.getInstance().createActionPopupMenu(contextMenuPlace, popupActionGroup)
        popupMenu.component.show(comp, x, y)
      }
    })

    tree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    tree.addTreeSelectionListener {
      mySelectedNodesData = tree.selectionPaths?.map{ makeNodeDataFromTreePath(explorer, it) } ?: listOf()
    }

    tree.addTreeWillExpandListener(object : TreeWillExpandListener {
      override fun treeWillExpand(event: TreeExpansionEvent) {
        val node = event.path.lastPathComponent
        if (myNodesToInvalidateOnExpand.contains(node)) {
          synchronized(myNodesToInvalidateOnExpand) {
            if (myNodesToInvalidateOnExpand.contains(node)) {
              myNodesToInvalidateOnExpand.remove(node)
              myStructure.invalidate(event.path, true)
            }
          }
        }
      }

      override fun treeWillCollapse(event: TreeExpansionEvent) {
      }
    })

    DoubleClickTreeMouseListener(tree) {
      if (isExpanded(it)) {
        collapsePath(it)
      } else {
        expandPath(it)
      }
    }.installOn(tree)

  }


}

class GlobalFileExplorerView(
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
  private val isDrag = AtomicBoolean(false)

  private val cutCopyPredicate: (NodeData) -> Boolean = {
    it.attributes?.isCopyPossible == true && (!isCut.get() || it.node !is UssDirNode || !it.node.isConfigUssPath)
  }
  init {
    myDragSource = GlobalExplorerViewDragSource(myTree, { mySelectedNodesData }, cutCopyPredicate, copyPasteSupport)
    myDropTarget = GlobalExplorerViewDropTarget(myTree, explorer, copyPasteSupport)
    DnDManager.getInstance().registerSource(myDragSource!!, myTree)
    DnDManager.getInstance().registerTarget(myDropTarget, myTree)
    DnDManager.getInstance().registerTarget(myDropTarget, ProjectView.getInstance(project).currentProjectViewPane.tree)
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
  inner class ExplorerCopyPasteSupport(val project: Project?): CopyPasteSupport {
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
      this@GlobalFileExplorerView.isCut.set(isCut)
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

    inner class ExplorerCutProvider(private val selectedNodesData: List<NodeData>? = null): CutProvider {

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

    fun getCutProvider (selectedTreePaths: List<TreePath?>): CutProvider {
      return ExplorerCutProvider(selectedTreePaths.mapNotNull { makeNodeDataFromTreePath(explorer, it) })
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

    internal val pastePredicate: (NodeData) -> Boolean = {
      it.attributes?.isPastePossible == true
    }

    internal fun registerDropTargetInProjectViewIfNeeded () {
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

    internal fun isPastePossible(destinationFiles: List<VirtualFile>?, sourceNodesData: List<NodeData>): Boolean {
      val destFiles = destinationFiles ?: mySelectedNodesData.mapNotNull { it.file }
      return bufferLock.withLock {
        getDestinationSourceFilePairs(
          sourceFiles = sourceNodesData.mapNotNull { it.file },
          destinationFiles = destFiles,
          isCut = isCut.get()
        ).isNotEmpty()
      }
    }

    fun isPastePossibleFromPath(destinationPaths: List<TreePath>, sourcePaths: List<TreePath?>): Boolean {
      return isPastePossible(
        destinationPaths.map { makeNodeDataFromTreePath(explorer, it).file as VirtualFile },
        sourcePaths.map { makeNodeDataFromTreePath(explorer, it) }
      )
    }

    internal fun isPastePossibleAndEnabled(destinationFiles: List<VirtualFile>?): Boolean {
      return isPastePossible(destinationFiles, if (dragAndDropCopyPasteBuffer.size > copyPasteBuffer.size) dragAndDropCopyPasteBuffer else copyPasteBuffer)
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

const val FILE_EXPLORER_CONTEXT_MENU = "File Explorer"

data class NodeData(
  val node: ExplorerTreeNode<*>,
  val file: MFVirtualFile?,
  val attributes: FileAttributes?
)

typealias FetchNode = FileFetchNode<*, *, *, *, *>

