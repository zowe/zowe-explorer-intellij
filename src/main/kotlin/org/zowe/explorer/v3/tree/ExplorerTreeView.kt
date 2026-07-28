/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
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
import org.zowe.explorer.v3.tree.nodes.LazyExpandable
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

/**
 * Base scroll pane component for explorer tree views (Files, JES, TSO).
 * Wraps a [DnDAwareTree] with left/right action toolbars, a context menu,
 * and tree expansion listeners that trigger lazy loading via [LazyExpandable].
 *
 * Subclasses define the action groups and are initialized through [initExplorerTreeView],
 * which assembles the toolbars, registers listeners, and returns a ready-to-use [JComponent].
 *
 * @param explorerName identifier used for toolbar action places and the context menu
 * @param explorerTreeModel the tree model driving the [DnDAwareTree]
 */
abstract class ExplorerTreeView(
  protected val explorerName: String,
  protected val explorerTreeModel: AbstractTreeModel
) : JBScrollPane(), Disposable {
  protected abstract val leftActionGroup: ActionGroup
  protected abstract val rightActionGroup: ActionGroup
  protected abstract val contextMenuGroup: ActionGroup
  private val contextMenuGroupPlace = explorerName
  protected val explorerDnDAwareTree by lazy { DnDAwareTree(explorerTreeModel) }

  fun initLeftActionToolbar(): ActionToolbar {
    return ActionManager.getInstance()
      .createActionToolbar("${explorerName}Left", leftActionGroup, true)
  }

  fun initRightActionToolbar(): ActionToolbar {
    return ActionManager.getInstance()
      .createActionToolbar("${explorerName}Right", rightActionGroup, true)
  }

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

    explorerDnDAwareTree.addMouseListener(object : java.awt.event.MouseAdapter() {
      override fun mouseClicked(e: java.awt.event.MouseEvent) {
        if (e.clickCount == 2 && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
          val row = explorerDnDAwareTree.getClosestRowForLocation(e.x, e.y)
          if (row < 0) return
          val path = explorerDnDAwareTree.getPathForRow(row) ?: return
          val node = (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? ExplorerTreeNode ?: return
          if (node.canNavigate()) {
            node.navigate(true)
          }
        }
      }
    })

    explorerDnDAwareTree.addTreeWillExpandListener(object : TreeWillExpandListener {
      override fun treeWillExpand(event: TreeExpansionEvent) {
        val defaultMutableTreeNode = event.path.lastPathComponent as? DefaultMutableTreeNode
        val explorerTreeNode = defaultMutableTreeNode?.userObject as? ExplorerTreeNode
        (explorerTreeNode?.nodeDescriptor as? LazyExpandable)?.expandNode(explorerTreeNode)
      }

      override fun treeWillCollapse(event: TreeExpansionEvent) {}
    })
  }

  fun initExplorerTreeView(): JComponent {
    return object : SimpleToolWindowPanel(true, true), Disposable {
      override fun dispose() {}

      init {
        val leftActionToolbar = initLeftActionToolbar()
        val rightActionToolbar = initRightActionToolbar()
          .apply {

          }
        leftActionToolbar.targetComponent = this
        rightActionToolbar.targetComponent = this
        toolbar = JPanel(BorderLayout()).apply {
          add(leftActionToolbar.component, BorderLayout.WEST)
          add(rightActionToolbar.component, BorderLayout.EAST)
        }
        setContent(this@ExplorerTreeView)
        explorerDnDAwareTree.isRootVisible = false
        explorerDnDAwareTree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true)
        registerTreeListeners()
        setViewportView(explorerDnDAwareTree)
      }
    }
  }
}
