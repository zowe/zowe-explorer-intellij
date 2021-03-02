package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.service
import java.util.concurrent.locks.ReentrantLock

abstract class FileCacheNode<Value : Any, R : Any, Q : Query<R>, File : VirtualFile, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNodeBase<*>,
  unit: U,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<Value, U>(value, project, parent, unit, treeStructure) {

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  private val fileFetchProvider
    get() = service<DataOpsManager>(explorer.componentManager)
      .getFileFetchProvider(requestClass, queryClass, vFileClass)


  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return lock(lock) {
      val childrenNodes = cachedChildren
        ?.toChildrenNodes() ?: listOf(LoadingNode(notNullProject, this, explorer, treeStructure)).also {
        query?.let { q ->
          fileFetchProvider.forceReloadAsync(q, fetchAdapter {}, project)
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

  abstract val query: Q?

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

fun ExplorerTreeNodeBase<*>.cleanCacheIfPossible() {
  if (this is FileCacheNode<*, *, *, *, *>) {
    cleanCache()
  }
}