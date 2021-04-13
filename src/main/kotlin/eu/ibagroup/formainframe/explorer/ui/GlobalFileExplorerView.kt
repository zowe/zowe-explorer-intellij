package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.*
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
import com.intellij.util.EditSourceOnDoubleClickHandler
import eu.ibagroup.formainframe.common.ui.DoubleClickTreeMouseListener
import eu.ibagroup.formainframe.common.ui.promisePath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileCacheListener
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.operations.*
import eu.ibagroup.formainframe.explorer.*
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.validation.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.Component
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import kotlin.concurrent.withLock

val FILE_EXPLORER_VIEW = DataKey.create<GlobalFileExplorerView>("fileExplorerView")

class GlobalFileExplorerView(
  internal val explorer: Explorer,
  project: Project,
  parentDisposable: Disposable,
  private val cutProviderUpdater: (List<VirtualFile>) -> Unit
) : JBScrollPane(), DataProvider, Disposable {

  internal var mySelectedNodesData: List<NodeData> by rwLocked(listOf())
  internal val myFsTreeStructure: FileExplorerTreeStructure
  internal val myStructure: StructureTreeModel<FileExplorerTreeStructure>
  internal val myTree: Tree
  internal val myNodesToInvalidateOnExpand = hashSetOf<Any>()

  private val dataOpsManager = explorer.componentManager.service<DataOpsManager>()

  private val ignoreVFileDeleteEvents = AtomicBoolean(false)

  internal fun getNodesByQueryAndInvalidate(
    query: Query<*, *>, collapse: Boolean = false, invalidate: Boolean = true
  ): Collection<ExplorerTreeNode<*>> {
    return myFsTreeStructure.findByPredicate {
      if (it is FetchNode) {
        it.query == query
      } else false
    }.distinct().onEach { foundNode ->
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
      FileExplorerTreeStructure(explorer, project).also { myFsTreeStructure = it },
      { o1, o2 ->
        if (o1 is WorkingSetNode && o2 is WorkingSetNode) {
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
        override fun onAdded(explorer: Explorer, unit: ExplorerUnit) {
          onAddDelete(explorer)
        }

        private fun onAddDelete(explorer: Explorer) {
          if (explorer == explorer) {
            myFsTreeStructure.findByValue(explorer).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun onChanged(explorer: Explorer, unit: ExplorerUnit) {
          if (explorer == explorer) {
            myFsTreeStructure.findByValue(unit).forEach {
              myStructure.invalidate(it, true)
            }
          }
        }

        override fun onDeleted(explorer: Explorer, unit: ExplorerUnit) {
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
                && this@GlobalFileExplorerView
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
      componentManager = explorer.componentManager,
      topic = ExplorerContent.CUT_BUFFER_CHANGES,
      handler = CutBufferListener { previousBufferState, currentBufferState ->
        (previousBufferState + currentBufferState).toSet().map {
          myFsTreeStructure.findByVirtualFile(it)
        }.flatten().toSet().forEach {
          myStructure.invalidate(it, true)
        }
      }
    )
  }

  private var treeModel: AsyncTreeModel

  private fun registerTreeListeners(tree: DnDAwareTree) {
    tree.addMouseListener(object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {
//        if (tree.getRowForLocation(x, y) == -1) {
//          return
//        }
        val popupActionGroup = DefaultActionGroup()
        popupActionGroup.add(
          ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ContextMenuGroup")
        )
        val popupMenu = ActionManager.getInstance().createActionPopupMenu(FILE_EXPLORER_CONTEXT_MENU, popupActionGroup)
        popupMenu.component.show(comp, x, y)
      }
    })

    tree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    tree.addTreeSelectionListener {
      mySelectedNodesData = tree.selectionPaths?.map {
        val descriptor = (it.lastPathComponent as DefaultMutableTreeNode).userObject as ExplorerTreeNode<*>
        val file = descriptor.virtualFile
        val attributes = if (file != null) {
          explorer.componentManager.service<DataOpsManager>().tryToGetAttributes(file)
        } else null
        NodeData(descriptor, file, attributes)
      } ?: listOf()
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

  override fun dispose() {
  }

  private val copyPasteSupport = object : CopyPasteSupport {

    private val bufferLock = ReentrantLock()

    @Volatile
    private var copyPasteBuffer = LinkedList<NodeData>()
    private val isCut = AtomicBoolean(true)

    private fun isCopyCutEnabledAndVisible(): Boolean {
      val nodes = mySelectedNodesData
      return nodes.any(cutCopyPredicate)
    }

    private val cutCopyPredicate: (NodeData) -> Boolean = {
      (it.attributes is RemoteDatasetAttributes && it.file?.isDirectory == false)
        || it.attributes is RemoteMemberAttributes
    }

    private fun performCopyCut(isCut: Boolean) {
      val nodes = mySelectedNodesData
      this.isCut.set(isCut)
      bufferLock.withLock {
        copyPasteBuffer = nodes.filter(cutCopyPredicate).apply {
          if (isCut) {
            mapNotNull { it.file }.also(cutProviderUpdater)
          }
        }.let { LinkedList(it) }
      }
    }

    override fun getCutProvider(): CutProvider {
      return object : CutProvider {
        override fun performCut(dataContext: DataContext) {
          performCopyCut(true)
        }

        override fun isCutEnabled(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible()
        }

        override fun isCutVisible(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible()
        }
      }
    }

    override fun getCopyProvider(): CopyProvider {
      return object : CopyProvider {
        override fun performCopy(dataContext: DataContext) {
          performCopyCut(false)
        }

        override fun isCopyEnabled(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible()
        }

        override fun isCopyVisible(dataContext: DataContext): Boolean {
          return isCopyCutEnabledAndVisible()
        }
      }
    }

    private val pastePredicate: (NodeData) -> Boolean = {
      it.attributes is RemoteDatasetAttributes && it.file?.isDirectory == true
    }

    private fun isPastePossibleAndEnabled(): Boolean {
      val nodes = mySelectedNodesData
      return bufferLock.withLock {
        getDestinationSourceFilePairs(
          sourceFiles = copyPasteBuffer.mapNotNull { it.file },
          destinationFiles = nodes.mapNotNull { it.file },
          isCut = isCut.get()
        ).isNotEmpty()
      }
    }

    fun getDestinationSourceFilePairs(
      sourceFiles: List<VirtualFile>,
      destinationFiles: List<VirtualFile>,
      isCut: Boolean
    ): List<Pair<VirtualFile, VirtualFile>> {
      return destinationFiles
        .map { destFile ->
          sourceFiles.map { Pair(destFile, it) }
        }.flatten()
        .filter {
          dataOpsManager.isOperationSupported(
            operation = MoveCopyOperation(
              source = it.second,
              destination = it.first,
              isMove = isCut,
              forceOverwriting = false,
              newName = null,
              dataOpsManager
            )
          )
        }
    }

    override fun getPasteProvider(): PasteProvider {
      return object : PasteProvider {
        override fun performPaste(dataContext: DataContext) {
          val pasteDestinationsNodes = mySelectedNodesData
            .filter(pastePredicate)

          bufferLock.withLock {
            val sourceFilesRaw = copyPasteBuffer.mapNotNull { it.file }
            val skipDestinationSourceList = mutableListOf<Pair<VirtualFile, VirtualFile>>()
            val overwriteDestinationSourceList = mutableListOf<Pair<VirtualFile, VirtualFile>>()

            val destinationSourceFilePairs = getDestinationSourceFilePairs(
              sourceFiles = sourceFilesRaw,
              destinationFiles = pasteDestinationsNodes.mapNotNull { it.file },
              isCut = isCut.get()
            )

            val pasteDestinations = destinationSourceFilePairs.map { it.first }.toSet().toList()
            val sourceFiles = destinationSourceFilePairs.map { it.second }.toSet().toList()

            if (isCut.get()) {
              showYesNoDialog(
                title = "Moving of ${sourceFiles.size} file(s)",
                message = "Do you want to move these files?",
                project = project
              ).let {
                if (!it) {
                  return@withLock
                }
              }
            }

            val conflicts = pasteDestinations
              .mapNotNull { destFile ->
                destFile.children
                  ?.mapNotNull conflicts@{ destChild ->
                    Pair(destFile, sourceFiles.find { it.name == destChild.name } ?: return@conflicts null)
                  }
              }
              .flatten()

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

            val operations = pasteDestinations.map { destFile ->
              sourceFiles.mapNotNull { sourceFile ->
                if (skipDestinationSourceList.contains(Pair(destFile, sourceFile))) {
                  return@mapNotNull null
                }
                MoveCopyOperation(
                  source = sourceFile,
                  destination = destFile,
                  isMove = isCut.get(),
                  forceOverwriting = overwriteDestinationSourceList.contains(Pair(destFile, sourceFile)),
                  newName = null,
                  dataOpsManager
                )
              }
            }.flatten()

            val filesToMoveTotal = operations.size
            val titlePrefix = if (isCut.get()) {
              "Moving"
            } else {
              "Copying"
            }
            runModalTask(
              title = "$titlePrefix $filesToMoveTotal file(s)",
              project = project,
              cancellable = true
            ) {
              operations.forEach { op ->
                it.text = "${op.source.name} to ${op.destination.name}"
                runCatching {
                  dataOpsManager.performOperation(
                    operation = op,
                    progressIndicator = it
                  )
                }.onSuccess {
                  if (isCut.get()) {
                    synchronized(copyPasteBuffer) {
                      copyPasteBuffer.removeIf { it.file == op.source }
                      cutProviderUpdater(copyPasteBuffer.mapNotNull { it.file })
                    }
                  }
                }.onFailure {
                  explorer.reportThrowable(it, project)
                }
                it.fraction = it.fraction + 1.0 / filesToMoveTotal
              }
              val nodesToRefresh = if (isCut.get()) {
                copyPasteBuffer + pasteDestinationsNodes
              } else {
                pasteDestinationsNodes
              }
              nodesToRefresh.forEach {
                it.node.cleanCacheIfPossible()
                myStructure.invalidate(it.node, true)
              }
            }
          }
        }

        override fun isPastePossible(dataContext: DataContext): Boolean {
          return isPastePossibleAndEnabled()
        }

        override fun isPasteEnabled(dataContext: DataContext): Boolean {
          return isPastePossibleAndEnabled()
        }
      }
    }

  }

  private val deleteProvider = object : DeleteProvider {
    override fun deleteElement(dataContext: DataContext) {
      val selected = mySelectedNodesData
      selected.map { it.node }.filterIsInstance<WorkingSetNode>()
        .forEach {
          if (showYesNoDialog(
              title = "Deletion of Working Set ${it.unit.name}",
              message = "Do you want to delete this Working Set from configs? Note: all data under it will be untouched",
              project = project,
              icon = AllIcons.General.QuestionDialog
            )
          ) {
            explorer.disposeUnit(it.unit)
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
          it.node is WorkingSetNode || it.node is DSMaskNode || (it.node is UssDirNode && it.node.isConfigUssPath)
        }.mapNotNull {
          val file = it.file ?: return@mapNotNull null
          val pathFiles = mutableListOf<MFVirtualFile>()
          var current: MFVirtualFile? = file
          while (current != null) {
            pathFiles.add(current)
            current = current.parent
          }
          Pair(it, pathFiles)
        }
      val nodeDataAndPathFiltered = nodeDataAndPaths.filter { orig ->
        nodeDataAndPaths.filter { orig.second.size > it.second.size }
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
              .forEach { it.cleanCache(false) }
//            files.asSequence().mapNotNull { it.parent }.toSet().map {
//              myFsTreeStructure.findByVirtualFile(it)
//            }.flatten().toSet().forEach {
//              it.cleanCacheIfPossible()
//              myStructure.invalidate(it, true)
//            }
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
        it.node is WorkingSetNode
          || it.node is DSMaskNode
          || (it.node is UssDirNode && it.node.isConfigUssPath)
          || deleteOperations.any { op -> dataOpsManager.isOperationSupported(op) }
      }
    }
  }

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId) -> mySelectedNodesData.map { it.node }.toTypedArray()
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
  val attributes: VFileInfoAttributes?
)

typealias FetchNode = FileFetchNode<*, *, *, *, *>

