package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.utils.locked
import eu.ibagroup.formainframe.utils.service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class FileFetchNode<Value : Any, R : Any, Q : Query<R, Unit>, File : VirtualFile, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<*>,
  unit: U,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<Value, U>(value, project, parent, unit, treeStructure) {

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  private val fileFetchProvider
    get() = explorer.componentManager.service<DataOpsManager>()
      .getFileFetchProvider(requestClass, queryClass, vFileClass)

  protected abstract fun makeFetchTaskTitle(query: Q): String

  private val loadingNode by lazy { listOf(LoadingNode(notNullProject, this, explorer, treeStructure)) }
  private val errorNode by lazy { listOf(ErrorNode(notNullProject, this, explorer, treeStructure)) }
  private val hasError = AtomicBoolean(false)

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return lock.withLock {
      val childrenNodes = cachedChildren
      if (childrenNodes == null) {
        val q = query
        if (q != null && fileFetchProvider.isCacheValid(q)) {
          val fetched = fileFetchProvider.getCached(q)
          if (fetched != null) {
            fetched.toChildrenNodes().also { cachedChildren = it }
          } else {
            runBackgroundableTask(
              title = makeFetchTaskTitle(q),
              project = project,
              cancellable = true
            ) {
              fileFetchProvider.reload(q, it)
            }
            loadingNode
          }
        } else {
          hasError.set(true)
          errorNode.also { cachedChildren = it }
        }
      } else {
        childrenNodes
      }
    }.toMutableSmartList()
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

  private var cachedChildren: List<AbstractTreeNode<*>>? by locked(null, lock)

  fun cleanCache(recursively: Boolean = true) {
    val children = cachedChildren
    if (!hasError.compareAndSet(true, false)) {
      cachedChildren = null
    }
    query?.let {
      fileFetchProvider.cleanCache(it)
    }
    if (recursively) {
      children?.forEach {
        if (it is FileFetchNode<*, *, *, *, *>) {
          it.cleanCache()
        }
      }
    }
  }

  protected abstract fun Collection<File>.toChildrenNodes(): List<AbstractTreeNode<*>>

  protected abstract val requestClass: Class<out R>

  protected abstract val queryClass: Class<out Query<*, *>>

  protected abstract val vFileClass: Class<out File>

}

fun ExplorerTreeNode<*>.cleanCacheIfPossible() {
  if (this is FileFetchNode<*, *, *, *, *>) {
    cleanCache()
  }
}