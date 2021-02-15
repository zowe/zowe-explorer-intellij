package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.dataOpsManager
import eu.ibagroup.formainframe.dataops.fetch.Query
import eu.ibagroup.formainframe.dataops.fetch.fetchAdapter
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.sendTopic
import java.util.concurrent.locks.ReentrantLock

abstract class FileCacheNode<Value : Any, R : Any, Q : Query<R>, File : VirtualFile, U : ExplorerUnit>(
  value: Value,
  unit: U,
  explorerViewSettings: ExplorerViewSettings
) : ExplorerUnitTreeNodeBase<Value, U>(value, unit, explorerViewSettings) {

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  private val fileFetchProvider
    get() = dataOpsManager.getFileFetchProvider(requestClass, queryClass, vFileClass)

  private fun sendInvalidationTopic() {
    needsToShowPlus = false
    sendTopic(FileExplorerContent.NODE_UPDATE)(this@FileCacheNode, true)
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return lock(lock) {
      val childrenNodes = cachedChildren?.toChildrenNodes() ?: listOf(LoadingNode(explorer, viewSettings)).also {
        query?.let { q ->
          fileFetchProvider.forceReloadAsync(q, unit.explorer.project, fetchAdapter {
              onFinish {
                needsToShowPlus = false
                sendTopic(FileExplorerContent.NODE_UPDATE)(this@FileCacheNode, true)
              }
            })
        }
      }
      childrenNodes.toMutableSmartList()
    }
  }

  override fun getLeafState(): LeafState {
    return if (needsToShowPlus) {
      LeafState.NEVER
    } else {
      LeafState.DEFAULT
    }
  }

  protected abstract val query: Q?

  @Volatile
  private var needsToShowPlus = true

  private val cachedChildren: Collection<File>?
    get() = query?.let { fileFetchProvider.getCached(it) }


  fun cleanCache() {
    query?.let {
      fileFetchProvider.cleanCache(it)
    }
  }

  protected abstract fun Collection<File>.toChildrenNodes(): List<AbstractTreeNode<*>>

  protected abstract val requestClass: Class<out R>

  protected abstract val queryClass: Class<out Query<*>>

  protected abstract val vFileClass: Class<out File>

}