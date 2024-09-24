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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.attributes.RemoteSpoolFileAttributes
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JobNode
import org.zowe.explorer.explorer.ui.JobPropertiesDialog
import org.zowe.explorer.explorer.ui.JobState
import org.zowe.explorer.explorer.ui.SpoolFileNode
import org.zowe.explorer.explorer.ui.SpoolFilePropertiesDialog
import org.zowe.explorer.explorer.ui.SpoolFileState
import org.zowe.explorer.explorer.ui.getExplorerView

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
