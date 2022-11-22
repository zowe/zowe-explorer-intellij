/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithWS
import eu.ibagroup.formainframe.explorer.ui.AddJobsFilterDialog
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.JES_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.JesWsNode

/**
 * Action for adding Job Filter from UI.
 * @author Valiantsin Krus
 */
class AddJobsFilterAction : JobsFilterAction() {

  /**
   * Is node conforms to the JesFilterNode and the JesWsNode types
   * @param node the node to check
   */
  override fun isNodeConformsToType(node: ExplorerTreeNode<*>?): Boolean {
    return super.isNodeConformsToType(node) || node is JesWsNode
  }

  /** Opens AddJobsFilterDialog and saves result. */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val owner = ws.connectionConfig?.let { CredentialService.instance.getUsernameByKey(it.uuid) } ?: ""
    val initialState = JobFilterStateWithWS(ws = ws, owner = owner)
    val dialog = AddJobsFilterDialog(e.project, initialState)
    if (dialog.showAndGet()) {
      ws.addMask(dialog.state.toJobsFilter())
    }
  }

}
