/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.explorer.actions.sort

import com.intellij.openapi.actionSystem.*
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.ui.*

abstract class SortAction<Node: ExplorerTreeNode<*, *>> : ToggleAction() {

  companion object {
    fun runRefreshAction(e: AnActionEvent) {
      val refreshActionInstance: AnAction = ActionManager.getInstance().getAction("eu.ibagroup.formainframe.explorer.actions.RefreshNodeAction")
      refreshActionInstance.actionPerformed(e)
    }
  }

  /**
   * Function gets the source view where the action is triggered
   * @param e
   * @return an instance of ExplorerTreeView
   */
  abstract fun getSourceView(e: AnActionEvent) : ExplorerTreeView<*, *, *>?

  /**
   * Function gets the source node on which the action is triggered
   * @param view
   * @return an instance of Node
   */
  abstract fun getSourceNode(view: ExplorerTreeView<*, *, *>) : Node?

  /**
   * Function performs the query update for the selected node and adds/removes the selected sort key
   * @param selectedNode
   * @param sortKey
   */
  abstract fun performQueryUpdateForNode(selectedNode: Node, sortKey: SortQueryKeys)

  /**
   * Function checks if the sort key is currently enabled for the particular node
   * @param selectedNode
   * @param sortKey
   * @return true if the sort key is currently enabled, false otherwise
   */
  abstract fun shouldEnableSortKeyForNode(selectedNode: Node, sortKey: SortQueryKeys) : Boolean

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = true
  }

  /**
   * If action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Tells that only UI component is affected
   */
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Action performed method to register the custom behavior when any Sort Key was clicked in UI
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = getSourceView(e) ?: return
    val selectedNode = getSourceNode(view) ?: return
    val sortKey = this.templateText?.uppercase()?.replace(" ", "_")?.let { SortQueryKeys.valueOf(it) }
      ?: throw Exception("Sort key for the selected action was not found.")
    if (isSelected(e)) return
    performQueryUpdateForNode(selectedNode, sortKey)

    // Create an instance of ActionEvent from the received sort action and trigger refresh action
    val actionEvent = AnActionEvent.createFromDataContext(e.place, null, e.dataContext)
    runRefreshAction(actionEvent)
  }

  /**
   * Custom isSelected method determines if the Sort Key is currently enabled or not. Updates UI by 'tick' mark
   */
  override fun isSelected(e: AnActionEvent): Boolean {
    val view = getSourceView(e) ?: return false
    val selectedNode = getSourceNode(view) ?: return false
    val sortKey = this.templateText?.uppercase()?.replace(" ", "_")?.let { SortQueryKeys.valueOf(it) } ?: return false
    return shouldEnableSortKeyForNode(selectedNode, sortKey)
  }

}
