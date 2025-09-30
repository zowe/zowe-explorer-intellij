/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.components.ExplorerTreeComponentService
import java.util.concurrent.atomic.AtomicReference

// TODO: doc
abstract class ExpandableNode(
  project: Project,
  nodeData: ExplorerTreeNodeData,
  parent: ExplorerTreeNode
) : ExplorerTreeNode(project, nodeData, parent) {
  enum class State {
    INIT,
    LOADING,
    LOADED
  }

  private val currentNodeTask = AtomicReference<Task.Backgroundable?>(null)

  @Volatile
  var nodeState: State = State.INIT
  protected val cachedChildren = mutableListOf<ExplorerTreeNode>()

  protected abstract fun fetchChildren(): List<ExplorerTreeNode>

  protected fun produceErrorChildren(e: Exception): List<ExplorerTreeNode> {
    return listOf(ErrorNode(project, e.message ?: "Unknown error", parent=this))
  }

  protected fun produceNoItemsFoundChildren(): List<ExplorerTreeNode> {
    return listOf(NoItemsFoundNode(project, parent=this))
  }

  protected fun produceLoadingChildren(): List<ExplorerTreeNode> {
    return listOf(LoadingNode(project, parent=this))
  }

  protected open fun startChildrenLoading() {
    // TODO: concurrent tasks interface
    if (nodeState == State.LOADING) return
    nodeState = State.LOADING

    val explorerComponent = ExplorerTreeComponentService.getService()
      .getFilesExplorerComponent(project)
    explorerComponent.invalidateNode(this)

    // TODO: implement cancel right after we asked to cancel
    val loadChildrenTask = object : Task.Backgroundable(
      project,
      "Loading children for ${nodeData.displayName}...",
      true
    ) {
      private var newChildren: List<ExplorerTreeNode>? = null
      private var error: Exception? = null

      override fun run(indicator: ProgressIndicator) {
        try {
          newChildren = fetchChildren()
        } catch (e: Exception) {
          if (indicator.isCanceled) return
          error = e
        }
      }

      override fun onSuccess() {
        cachedChildren.clear()
        if (newChildren != null) {
          cachedChildren.addAll(newChildren ?: produceNoItemsFoundChildren())
        } else if (error != null) {
          val errorChildren = produceErrorChildren(error ?: Exception("Unknown error"))
          cachedChildren.addAll(errorChildren)
        } else {
          cachedChildren.addAll(produceNoItemsFoundChildren())
        }
        nodeState = State.LOADED
        explorerComponent.invalidateNode(this@ExpandableNode)
        currentNodeTask.set(null)
      }

      override fun onCancel() {
        cachedChildren.clear()
        cachedChildren.addAll(produceErrorChildren(Exception("Loading cancelled")))
        nodeState = State.LOADED

        explorerComponent.invalidateNode(this@ExpandableNode)
        currentNodeTask.set(null)
      }
    }

    currentNodeTask.set(loadChildrenTask)
    ProgressManager.getInstance().run(loadChildrenTask)
  }

  override fun getChildren(): List<ExplorerTreeNode> {
    return when (nodeState) {
      State.INIT -> produceNoItemsFoundChildren()
      State.LOADING -> produceLoadingChildren()
      State.LOADED -> cachedChildren
    }
  }

  fun expandNode() {
    if (nodeState == State.INIT) {
      startChildrenLoading()
    }
  }
}