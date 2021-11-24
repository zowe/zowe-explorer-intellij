package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.*
import com.intellij.ide.dnd.*
import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
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
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.common.ui.DoubleClickTreeMouseListener
import eu.ibagroup.formainframe.common.ui.makeNodeDataFromTreePath
import eu.ibagroup.formainframe.common.ui.promisePath
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
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
import javax.swing.tree.DefaultMutableTreeNode
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
  protected val cutProviderUpdater: (List<VirtualFile>) -> Unit
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
  private val copyPasteSupport = ExplorerCopyPasteSupport(project)

  private var myDragSource: DnDSource?
  private var myDropTarget: DnDTarget?



  private val isCut = AtomicBoolean(true)
  private val isDrag = AtomicBoolean(false)

  private val cutCopyPredicate: (NodeData) -> Boolean = {
    it.attributes?.isCopyPossible == true && (!isCut.get() || it.node !is UssDirNode || !it.node.isConfigUssPath)
  }
  init {
    myDragSource = GlobalExplorerViewDragSource(myTree, { mySelectedNodesData }, cutCopyPredicate)
    myDropTarget = GlobalExplorerViewDropTarget(myTree, explorer, copyPasteSupport)
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
  inner class ExplorerCopyPasteSupport(val project: Project?): CopyPasteSupport {
    private val bufferLock = ReentrantLock()

    @Volatile
    var copyPasteBuffer = LinkedList<NodeData>()

    @Volatile
    var dragAndDropCopyPasteBuffer = LinkedList<NodeData>()

    private fun isCopyCutEnabledAndVisible(selectedNodesData: List<NodeData>? = null): Boolean {
      val nodes = selectedNodesData ?: mySelectedNodesData
      return nodes.all(cutCopyPredicate)
    }


    private fun performCopyCut(isCut: Boolean, selectedNodesData: List<NodeData>? = null) {
      val nodes = selectedNodesData ?: mySelectedNodesData
      this@GlobalFileExplorerView.isCut.set(isCut)
      bufferLock.withLock {
        val buffer = nodes.filter(cutCopyPredicate).apply {
          if (selectedNodesData == null) {
            if (isCut) {
              mapNotNull { it.file }.also(cutProviderUpdater)
            } else {
              cutProviderUpdater(emptyList())
            }
          }
          forEach {
            it.file?.let { file ->
              service<DataOpsManager>().tryToGetAttributes(file)?.let { attrs ->
                service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attrs, FileAction.COPY))
              }
            }
          }
        }.let { LinkedList(it) }
        if (selectedNodesData == null) {
          copyPasteBuffer = buffer
        } else {
          dragAndDropCopyPasteBuffer = buffer
        }
      }
    }

    inner class ExplorerCutProvider(private val selectedNodesData: List<NodeData>? = null): CutProvider {

      override fun performCut(dataContext: DataContext) {
        performCopyCut(true, selectedNodesData)
        //TODO("add analytics")
      }

      override fun isCutEnabled(dataContext: DataContext): Boolean {
        return isCopyCutEnabledAndVisible(selectedNodesData)
      }

      override fun isCutVisible(dataContext: DataContext): Boolean {
        return isCopyCutEnabledAndVisible(selectedNodesData)
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
          performCopyCut(false)
          //TODO("add analytics")
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
      it.attributes?.isPastePossible == true
    }


    private fun isPastePossible(destinationsNodesData: List<NodeData>?, sourceNodesData: List<NodeData>): Boolean {
      val nodes = destinationsNodesData ?: mySelectedNodesData
      return bufferLock.withLock {
        getDestinationSourceFilePairs(
          sourceFiles = sourceNodesData.mapNotNull { it.file },
          destinationFiles = nodes.mapNotNull { it.file },
          isCut = isCut.get()
        ).isNotEmpty()
      }
    }

    fun isPastePossibleFromPath(destinationPaths: List<TreePath>, sourcePaths: List<TreePath?>): Boolean {
      return isPastePossible(
        destinationPaths.map { makeNodeDataFromTreePath(explorer, it) },
        sourcePaths.map { makeNodeDataFromTreePath(explorer, it) }
      )
    }

    private fun isPastePossibleAndEnabled(destinationsNodesData: List<NodeData>?): Boolean {
      return isPastePossible(destinationsNodesData, if (dragAndDropCopyPasteBuffer.size > copyPasteBuffer.size) dragAndDropCopyPasteBuffer else copyPasteBuffer)
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
              dataOpsManager
            )
          )
        }
    }

    override fun getPasteProvider(): PasteProvider {
      return ExplorerPasteProvider()
    }

    fun getPasteProvider(destinationNodes: List<TreePath>): PasteProvider {
      return ExplorerPasteProvider(destinationNodes.map{ makeNodeDataFromTreePath(explorer, it) })
    }


    inner class ExplorerPasteProvider(private val destinationsNodesData: List<NodeData>? = null) : PasteProvider {
      override fun performPaste(dataContext: DataContext) {
        val selectedNodesData = destinationsNodesData ?: mySelectedNodesData
        val pasteDestinationsNodesData = selectedNodesData
          .filter(pastePredicate)

        bufferLock.withLock {
          val sourceFilesRaw = if (destinationsNodesData == null) {
            copyPasteBuffer.mapNotNull { it.file }
          }
          else {
            dragAndDropCopyPasteBuffer.mapNotNull { it.file }
          }

          val skipDestinationSourceList = mutableListOf<Pair<VirtualFile, VirtualFile>>()
          val overwriteDestinationSourceList = mutableListOf<Pair<VirtualFile, VirtualFile>>()

          val destinationSourceFilePairs = getDestinationSourceFilePairs(
            sourceFiles = sourceFilesRaw,
            destinationFiles = pasteDestinationsNodesData.mapNotNull { it.file },
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
                  Pair(destFile, sourceFiles.find { source ->
                    val sourceAttributes = dataOpsManager.tryToGetAttributes(source)
                    val destAttributes = dataOpsManager.tryToGetAttributes(destChild)
                    if (
                      destAttributes is RemoteMemberAttributes &&
                      sourceAttributes is RemoteUssAttributes
                    ) {
                      val memberName = sourceAttributes.name.filter { it.isLetterOrDigit() }.take(8).toUpperCase()
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
                sourceAttributes is RemoteUssAttributes
              }
              sourceUssAttributes.map { Pair(destFile, it) }.ifEmpty { null }
            }
          }.flatten()

          if (ussToPdsWarnings.isNotEmpty() &&
            !showYesNoDialog(
              "Uss File To Pds Placing",
              "You are about to place uss file to Pds. All lines exceeding the record length will be truncated.",
              null,
              "Ok",
              "Skip this files",
              AllIcons.General.WarningDialog
            )
          ) {
            skipDestinationSourceList.addAll(ussToPdsWarnings)
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
            fun List<MoveCopyOperation>.collectByFile(
              fileChooser: (MoveCopyOperation) -> VirtualFile
            ): List<VirtualFile> {
              return map(fileChooser)
                .distinct()
                .getMinimalCommonParents()
                .toList()
            }

            val destinationFilesToRefresh = operations.collectByFile { it.destination }
            val sourceFilesToRefresh = if (isCut.get()) {
              operations.collectByFile { it.source }
            } else {
              emptyList()
            }
            val destinationNodes = destinationFilesToRefresh
              .map { myFsTreeStructure.findByVirtualFile(it) }
              .flatten()
              .distinct()
            val nodesToRefresh = if (isCut.get()) {
              val sourceNodesToRefresh = sourceFilesToRefresh
                .map { file -> myFsTreeStructure.findByVirtualFile(file).map { it.parent } }
                .flatten()
                .filterNotNull()
                .distinct()
              destinationNodes.plus(sourceNodesToRefresh)
            } else {
              destinationNodes
            }

            nodesToRefresh.forEach { node ->
              // node.cleanCacheIfPossible()
              myFsTreeStructure.findByPredicate { foundNode ->
                if (foundNode is FetchNode && node is FetchNode) {
                  foundNode.query == node.query
                } else false
              }.onEach { foundNode ->
                foundNode.cleanCacheIfPossible()
              }.onEach { foundNode ->
                myStructure.invalidate(foundNode, true)
              }

            }

          }
        }
      }

      override fun isPastePossible(dataContext: DataContext): Boolean {
        return isPastePossibleAndEnabled(destinationsNodesData)
      }

      override fun isPasteEnabled(dataContext: DataContext): Boolean {
        return isPastePossibleAndEnabled(destinationsNodesData)
      }
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

