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

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JobNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.ui.build.jobs.JOB_ADDED_TOPIC
import eu.ibagroup.formainframe.utils.sendTopic

/** An action to view a process of running job in the Jobs Tool Window */
class ViewJobAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /** View a process of running job on click in the JES Explorer */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return
    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is ExplorerTreeNode<*, *>) {
      val virtualFile = node.virtualFile
      if (virtualFile != null) {
        val dataOpsManager = DataOpsManager.getService()
        val attributes: RemoteJobAttributes =
          dataOpsManager.tryToGetAttributes(virtualFile)?.clone() as RemoteJobAttributes

        val project = e.project ?: return
        sendTopic(JOB_ADDED_TOPIC, project).viewed(
          project,
          attributes.requesters[0].connectionConfig,
          virtualFile.filenameInternal,
          attributes.jobInfo
        )
      }
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /** Able to click only on a Job Node */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1 && node is JobNode
  }

}
