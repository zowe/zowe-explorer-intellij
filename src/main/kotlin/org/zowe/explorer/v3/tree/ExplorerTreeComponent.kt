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

package org.zowe.explorer.v3.tree

import com.intellij.openapi.Disposable
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.concurrency.Promise
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import javax.swing.JComponent
import javax.swing.tree.TreePath

// TODO: doc
abstract class ExplorerTreeComponent : Disposable {
  protected abstract val explorerTreeStructure: ExplorerTreeStructure
  protected val explorerStructureTreeModel by lazy { StructureTreeModel(explorerTreeStructure, this) }
  protected val explorerAsyncTreeModel by lazy { AsyncTreeModel(explorerStructureTreeModel, false, this) }
  protected abstract val explorerTreeView: ExplorerTreeView
  val explorerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  abstract val explorerName: String
  val isLockable = true

  val selectedNodes: List<ExplorerTreeNode>
    get() {
      return explorerTreeView.selectedNodes
    }

  fun invalidateNode(node: ExplorerTreeNode, withChildren: Boolean = true): Promise<TreePath> {
    return explorerStructureTreeModel.invalidate(node, withChildren)
  }

  fun initExplorerTreeComponent(): JComponent {
    return explorerTreeView.initExplorerTreeView()
  }

  override fun dispose() {
    explorerScope.cancel()
    explorerTreeView.dispose()
    explorerAsyncTreeModel.dispose()
    explorerStructureTreeModel.dispose()
  }
}
