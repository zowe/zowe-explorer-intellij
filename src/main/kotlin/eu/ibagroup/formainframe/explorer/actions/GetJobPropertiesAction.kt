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
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JobNode
import eu.ibagroup.formainframe.explorer.ui.JobPropertiesDialog
import eu.ibagroup.formainframe.explorer.ui.JobState
import eu.ibagroup.formainframe.explorer.ui.SpoolFileNode
import eu.ibagroup.formainframe.explorer.ui.SpoolFilePropertiesDialog
import eu.ibagroup.formainframe.explorer.ui.SpoolFileState
import eu.ibagroup.formainframe.explorer.ui.getExplorerView

/** Action to get job or spool file properties*/
class GetJobPropertiesAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /** Create properties dialog depending on received attributes*/
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return
    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is ExplorerTreeNode<ConnectionConfig, *>) {
      val virtualFile = node.virtualFile
      if (virtualFile != null) {
        val dataOpsManager = DataOpsManager.getService()
        when (val attributes = dataOpsManager.tryToGetAttributes(virtualFile)?.clone()) {
          is RemoteJobAttributes -> {
            val dialog = JobPropertiesDialog.create(e.project, JobState(attributes))
            dialog.showAndGet()
          }

          is RemoteSpoolFileAttributes -> {
            val dialog = SpoolFilePropertiesDialog.create(e.project, SpoolFileState(attributes))
            dialog.showAndGet()
          }
        }
      }
    }

  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /** Make action visible only for JES explorer*/
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1
      && (node is JobNode
      || node is SpoolFileNode)
  }
}
