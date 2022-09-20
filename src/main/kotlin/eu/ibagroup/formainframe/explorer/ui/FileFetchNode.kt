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

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.dataops.BatchedRemoteQuery
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.locked
import eu.ibagroup.formainframe.utils.service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// TODO: doc
/** Another unnecessary abstraction (?) to represent tree node */
abstract class FileFetchNode<Value : Any, R : Any, Q : Query<R, Unit>, File : VirtualFile, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<*>,
  unit: U,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<Value, U>(value, project, parent, unit, treeStructure) {

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()
  /** current cache will not be cleaned if this value is true. It was made for fetching files bi batches. */
  var needToLoadMore = false

  private val fileFetchProvider
    get() = explorer.componentManager.service<DataOpsManager>()
      .getFileFetchProvider(requestClass, queryClass, vFileClass)

  protected abstract fun makeFetchTaskTitle(query: Q): String

  private val loadingNode by lazy { LoadingNode(notNullProject, this, explorer, treeStructure) }

  private fun errorNode(text: String): List<ErrorNode> {
    return listOf(ErrorNode(notNullProject, this, explorer, treeStructure, text = text))
  }

  private val hasError = AtomicBoolean(false)

  private val connectionError = "Error: Check connection"

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return lock.withLock {
      val childrenNodes = cachedChildren
      if (childrenNodes == null) {
        val q = fileFetchProvider.getRealQueryInstance(query) ?: query
        if (q != null && fileFetchProvider.isCacheValid(q)) {
          val fetched = fileFetchProvider.getCached(q)?.toMutableList()
          if (fetched != null && !needToLoadMore) {
            fetched.toChildrenNodes().toMutableList().apply {
              val batchedQ = q.castOrNull<BatchedRemoteQuery<*>>()
              if (batchedQ?.fetchNeeded == true) {
                val itemsLeft = batchedQ.totalRows?.let { it - batchedQ.alreadyFetched }
                add(LoadMoreNode(notNullProject, this@FileFetchNode, explorer, treeStructure, itemsLeft))
              }
            }.also { cachedChildren = it }
          } else {
            needToLoadMore = false
            runBackgroundableTask(
              title = makeFetchTaskTitle(q),
              project = project,
              cancellable = true
            ) {
              fileFetchProvider.reload(q, it)
            }
            (fetched?.toChildrenNodes()?.toMutableList() ?: mutableListOf()).apply { add(loadingNode) }
          }
        } else {
          hasError.set(true)
          errorNode(
            if (unit.connectionConfig == null) {
              connectionError
            } else {
              q?.let { it1 -> fileFetchProvider.getFetchedErrorMessage(it1) } ?: message("title.error")
            }
          ).also {
            cachedChildren = it
          }
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

  fun cleanCache(recursively: Boolean = true, cleanFetchProviderCache: Boolean = true, cleanBatchedQuery: Boolean = false) {
    val children = cachedChildren
    if (!hasError.compareAndSet(true, false)) {
      cachedChildren = null
    }
    if (cleanFetchProviderCache) {
      query?.let {
        fileFetchProvider.cleanCache(it)
      }
    }
    if (cleanBatchedQuery) {
      fileFetchProvider.getRealQueryInstance(query)?.castOrNull<BatchedRemoteQuery<*>>()?.clear()
    }
    if (recursively) {
      children?.forEach {
        if (it is FileFetchNode<*, *, *, *, *>) {
          it.cleanCache(cleanBatchedQuery = cleanBatchedQuery)
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
