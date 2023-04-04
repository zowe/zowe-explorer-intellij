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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.service

/** Action to get job or spool file properties*/
class GetJobPropertiesAction : AnAction() {

  /** Create properties dialog depending on received attributes*/
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: return
    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is ExplorerTreeNode<ConnectionConfig, *>) {
      val virtualFile = node.virtualFile
      if (virtualFile != null) {
        val dataOpsManager = node.explorer.componentManager.service<DataOpsManager>()
        when (val attributes = dataOpsManager.tryToGetAttributes(virtualFile)?.clone()) {
          is RemoteJobAttributes -> {
            val dialog = JobPropertiesDialog(e.project, JobState(attributes))
            dialog.showAndGet()
          }
          is RemoteSpoolFileAttributes -> {
            val dialog = SpoolFilePropertiesDialog(e.project, SpoolFileState(attributes))
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
