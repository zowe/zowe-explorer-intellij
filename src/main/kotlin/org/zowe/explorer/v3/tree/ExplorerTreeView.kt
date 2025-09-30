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

import com.intellij.ide.dnd.aware.DnDAwareTree
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.tree.AbstractTreeModel
import org.zowe.explorer.v3.tree.nodes.ExpandableNode
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import java.awt.Component
import javax.swing.JComponent
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

// TODO: doc
abstract class ExplorerTreeView(
  protected val explorerName: String,
  protected val explorerTreeModel: AbstractTreeModel
) : JBScrollPane(), Disposable {
  protected abstract val actionGroup: ActionGroup
  protected abstract val contextMenuGroup: ActionGroup
  private val contextMenuGroupPlace = explorerName
  protected val explorerDnDAwareTree by lazy { DnDAwareTree(explorerTreeModel) }

  abstract fun initActionToolbar(): ActionToolbar

  val selectedNodes: List<ExplorerTreeNode>
    get() {
      val paths = explorerDnDAwareTree.selectionPaths ?: return listOf<ExplorerTreeNode>()
      return paths.mapNotNull { (it.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? ExplorerTreeNode }
    }

  // TODO: doc update
  // TODO: finalize
  /**
   * Register the Drag'n'Drop tree events listeners.
   * These are both mouse listeners, and the other tree listeners
   */
  private fun registerTreeListeners() {
    explorerDnDAwareTree.addMouseListener(object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {
        val popupActionGroup = DefaultActionGroup()
        popupActionGroup.add(contextMenuGroup)
        val popupMenu = ActionManager.getInstance()
          .createActionPopupMenu(contextMenuGroupPlace, popupActionGroup)
        popupMenu.component
          .show(comp, x, y)
      }
    })

    explorerDnDAwareTree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION

    explorerDnDAwareTree.addTreeWillExpandListener(object : TreeWillExpandListener {
      override fun treeWillExpand(event: TreeExpansionEvent) {
        val defaultMutableTreeNode = event.path.lastPathComponent as? DefaultMutableTreeNode
        val expandableNode = defaultMutableTreeNode?.userObject as? ExpandableNode
        expandableNode?.expandNode()
      }

      override fun treeWillCollapse(event: TreeExpansionEvent) {}
    })
  }

  fun initExplorerTreeView(): JComponent {
    return object : SimpleToolWindowPanel(true, true), Disposable {
      private val actionToolbar: ActionToolbar = initActionToolbar()

      override fun dispose() {}

      init {
        actionToolbar.targetComponent = this
        toolbar = actionToolbar.component
        setContent(this@ExplorerTreeView)
        explorerDnDAwareTree.isRootVisible = false
        explorerDnDAwareTree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
        registerTreeListeners()
        setViewportView(explorerDnDAwareTree)
      }
    }
  }
}
