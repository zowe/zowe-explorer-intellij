/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState
import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.common.message
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Query
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.utils.locked
import org.zowe.explorer.utils.service
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

  //  private val errorNode by lazy { listOf(ErrorNode(notNullProject, this, explorer, treeStructure, text = fileFetchProvider.getFetchedErrorMessage())) }
  private fun errorNode(text: String): List<ErrorNode> {
    return listOf(ErrorNode(notNullProject, this, explorer, treeStructure, text = text))
  }

  private val hasError = AtomicBoolean(false)

  private val connectionError = "Error: Check connection"

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
