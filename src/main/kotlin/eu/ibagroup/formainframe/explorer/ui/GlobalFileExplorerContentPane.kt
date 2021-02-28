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
import com.intellij.util.EditSourceOnDoubleClickHandler
import eu.ibagroup.formainframe.common.ui.findCommonParentPath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileCacheListener
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.MoveCopyOperation
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerListener
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.Component
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel
import kotlin.concurrent.withLock

class GlobalFileExplorerContentPane(
  private val explorer: Explorer,
  project: Project,
  parentDisposable: Disposable,
  private val cutProviderUpdater: (List<VirtualFile>) -> Unit
) : JBScrollPane(), DataProvider, Disposable {

  private val dataOpsManager = service<DataOpsManager>(explorer.componentManager)

  private val ignoreVFileDeleteEvents = AtomicBoolean(false)

  init {
    Disposer.register(parentDisposable, this)
    subscribe(
      componentManager = explorer.componentManager,
      topic = Explorer.UNITS_CHANGED,
      handler = object : ExplorerListener {
        override fun onAdded(explorer: Explorer, unit: ExplorerUnit) {
          onAddDelete(explorer)
        }

        private fun onAddDelete(explorer: Explorer) {
          if (explorer == this@GlobalFileExplorerContentPane.explorer) {
            fsTreeStructure?.findByValue(explorer)?.forEach {
              structure?.invalidate(it, true)
            }
          }
        }

        override fun onChanged(explorer: Explorer, unit: ExplorerUnit) {
          if (explorer == this@GlobalFileExplorerContentPane.explorer) {
            fsTreeStructure?.findByValue(unit)?.forEach {
              structure?.invalidate(it, true)
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
            val nodes = fsTreeStructure?.findByVirtualFile(it.file ?: return@mapNotNull null)
            when {
              it is VFileContentChangeEvent || it is VFilePropertyChangeEvent -> {
                nodes
              }
              it is VFileDeleteEvent
                && this@GlobalFileExplorerContentPane
                .ignoreVFileDeleteEvents
                .compareAndSet(true, true) -> {
                null
              }
              else -> {
                nodes?.mapNotNull { n -> n.parent }
              }
            }
          }.flatten().forEach { fileNode ->
            fileNode.cleanCacheIfPossible()
            structure?.invalidate(fileNode, true)
          }
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = explorer.componentManager,
      topic = FileFetchProvider.CACHE_UPDATED,
      handler = object : FileCacheListener {
        override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> onCacheUpdated(
          query: Q,
          files: Collection<File>
        ) {
          fsTreeStructure?.findByPredicate {
            if (it is FileCacheNode<*, *, *, *, *>) {
              it.query == query
            } else false
          }?.forEach {
            structure?.invalidate(it, true)
          }
        }
      },
      disposable = this
    )
    subscribe(
      componentManager = explorer.componentManager,
      topic = ExplorerContent.CUT_BUFFER_CHANGES,
      handler = CutBufferListener { previousBufferState, currentBufferState ->
        (previousBufferState + currentBufferState).toSet().mapNotNull {
          fsTreeStructure?.findByVirtualFile(it)
        }.flatten().toSet().forEach {
          structure?.invalidate(it, true)
        }
      }
    )
  }

  private var tree: DnDAwareTree? = null

  private var treeModel: AsyncTreeModel? = null

  private var selectedNodesData: List<NodeData>? = null

  private var fsTreeStructure: FileExplorerTreeStructure? = null

  private var structure: StructureTreeModel<FileExplorerTreeStructure>? =
    StructureTreeModel(
      FileExplorerTreeStructure(explorer, project).also { fsTreeStructure = it },
      this
    ).also { stm ->
      treeModel = AsyncTreeModel(stm, false, this).also {
        tree = DnDAwareTree(it).apply { isRootVisible = false }.also { t ->
          setViewportView(t)
          registerTreeListeners(t)
        }
      }
    }

  private fun registerTreeListeners(tree: DnDAwareTree) {
    tree.addMouseListener(object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {
        if (tree.getRowForLocation(x, y) == -1) {
          return
        }
        val popupActionGroup = DefaultActionGroup()
        popupActionGroup.add(
          ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.ContextMenuGroup")
        )
        val popupMenu = ActionManager.getInstance().createActionPopupMenu(FILE_EXPLORER_PLACE, popupActionGroup)
        popupMenu.component.show(comp, x, y)
      }
    })

    tree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    tree.addTreeSelectionListener {
      selectedNodesData = tree.selectionPaths?.map {
        val descriptor = (it.lastPathComponent as DefaultMutableTreeNode).userObject as ExplorerTreeNodeBase<*>
        val file = descriptor.virtualFile
        val attributes = if (file != null) {
          service<DataOpsManager>(explorer.componentManager).tryToGetAttributes(file)
        } else null
        NodeData(descriptor, file, attributes)
      }
    }

    EditSourceOnDoubleClickHandler.TreeMouseListener(tree).installOn(tree)

  }

  override fun dispose() {
    tree = null
    structure = null
    treeModel = null
    fsTreeStructure = null
  }

  private val copyPasteSupport = object : CopyPasteSupport {

    private val bufferLock = ReentrantLock()

    @Volatile
    private var copyPasteBuffer = LinkedList<NodeData>()
    private val isCut = AtomicBoolean(true)

    private fun isCopyCutEnabledAndVisible(): Boolean {
      val nodes = selectedNodesData ?: return false
      return nodes.any(cutCopyPredicate)
    }

    private val cutCopyPredicate: (NodeData) -> Boolean = {
      (it.attributes is RemoteDatasetAttributes && it.file?.isDirectory == false)
        || it.attributes is RemoteMemberAttributes
    }

    private fun performCopyCut(isCut: Boolean) {
      val nodes = selectedNodesData ?: return
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
      val nodes = selectedNodesData ?: return false
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
          val pasteDestinationsNodes = selectedNodesData
            ?.filter(pastePredicate) ?: return

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
              operations.parallelStream().forEach { op ->
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
                structure?.invalidate(it.node, true)
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
      val selected = selectedNodesData ?: return
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
        service<DataOpsManager>(explorer.componentManager).isOperationSupported(
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
              .parallelStream()
              .forEach { op ->
                it.text = "Deleting file ${op.file.name}"
                runCatching {
                  dataOpsManager.performOperation(op, it)
                }.onFailure { explorer.reportThrowable(it, project) }
                it.fraction = it.fraction + 1.0 / files.size
              }
            files.asSequence().mapNotNull { it.parent }.toSet().mapNotNull {
              fsTreeStructure?.findByVirtualFile(it)
            }.flatten().toSet().forEach {
              it.cleanCacheIfPossible()
              structure?.invalidate(it, true)
            }
          }
        }
      }
    }

    override fun canDeleteElement(dataContext: DataContext): Boolean {
      val selected = selectedNodesData ?: return false
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
      SELECTED_NODES.`is`(dataId) -> selectedNodesData
      CommonDataKeys.NAVIGATABLE_ARRAY.`is`(dataId) -> selectedNodesData?.map { it.node }?.toTypedArray()
      PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> copyPasteSupport.copyProvider
      PlatformDataKeys.CUT_PROVIDER.`is`(dataId) -> copyPasteSupport.cutProvider
      PlatformDataKeys.PASTE_PROVIDER.`is`(dataId) -> copyPasteSupport.pasteProvider
      PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId) -> deleteProvider
      else -> null
    }
  }

}

fun Collection<NodeData>.filterNodesBeneath(): Collection<NodeData> {
  val withPathLength = map { Pair(it, it.node.path.pathCount) }
  return withPathLength.filter { pair ->
    withPathLength
      .filter { pair.second > it.second }
      .none { it.first.node.path.findCommonParentPath(pair.first.node.path) != pair.first.node.path }
  }.map { it.first }
}

val SELECTED_NODES: DataKey<List<NodeData>> = DataKey.create("currentNode")

val FILE_EXPLORER_PLACE = "File Explorer"

data class NodeData(
  val node: ExplorerTreeNodeBase<*>,
  val file: MFVirtualFile?,
  val attributes: VFileInfoAttributes?
)