package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.subscribe

fun interface NodeListener : (Any, Boolean) -> Unit {
  override operator fun invoke(node: Any, structure: Boolean)
}

class FileExplorerContent(explorer: Explorer, parentDisposable: Disposable) : JBScrollPane(), Disposable {

  companion object {
    @JvmStatic
    val NODE_UPDATE = Topic.create("nodeUpdate", NodeListener::class.java)
  }

  init {
    Disposer.register(parentDisposable, this)
    subscribe(ConfigService.CONFIGS_CHANGED, eventAdaptor<WorkingSetConfig> {
      onAdd { structure?.invalidate() }
      onDelete { structure?.invalidate() }
      onUpdate { _, new ->
        structure?.invalidate()
//        treeModel?.accept {
//          return@accept when (it.lastPathComponent) {
//
//          }
//        }
      }
    })
    subscribe(NODE_UPDATE, NodeListener { node, structure -> this.structure?.invalidate(node, structure)  }, this)
//    subscribe(FileFetchProvider.CACHE_UPDATED, object : FileCacheListener {
//      override fun <R : Any, Q : Query<R>, File : VirtualFile> onCacheUpdated(query: Q, files: Collection<File>) {
//        structure?.invalidate()
//        //invalidator(query.request, structure = true, stopIfFound = false)
//      }
//    })
  }

  private var tree: DnDAwareTree? = null

  private var treeModel: AsyncTreeModel? = null

  private var structure: StructureTreeModel<FileExplorerTreeStructure>? =
    StructureTreeModel(FileExplorerTreeStructure(explorer), this).also { stm ->
      treeModel = AsyncTreeModel(stm, false, this).also {
        tree = DnDAwareTree(it).apply { isRootVisible = false }.also { t ->
          setViewportView(t)
        }
      }
    }

  override fun dispose() {
    tree = null
    structure = null
  }

}