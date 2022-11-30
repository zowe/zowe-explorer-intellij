/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.service

/**
 * Action for displaying properties of files on UI in dialog by clicking item in explorer context menu.
 * @author Valiantsin Krus.
 */
class GetFilePropertiesAction : AnAction() {

  /** Shows dialog with properties depending on type of the file selected by user. */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val node = view.mySelectedNodesData.getOrNull(0)?.node
    if (node is ExplorerTreeNode<*>) {
      val virtualFile = node.virtualFile
      if (virtualFile != null) {
        val dataOpsManager = node.explorer.componentManager.service<DataOpsManager>()
        when (val attributes = dataOpsManager.tryToGetAttributes(virtualFile)?.clone()) {
          is RemoteDatasetAttributes -> {
            val dialog = DatasetPropertiesDialog(e.project, DatasetState(attributes))
            dialog.showAndGet()
          }

          is RemoteUssAttributes -> {
            val dialog = UssFilePropertiesDialog(e.project, UssFileState(attributes))
            dialog.showAndGet()
          }

          is RemoteMemberAttributes -> {
            val dialog = MemberPropertiesDialog(e.project, MemberState(attributes))
            dialog.showAndGet()
          }
        }
      }
    }

  }

  /** Action is available in all time and not only after indexing process will finish. */
  override fun isDumbAware(): Boolean {
    return true
  }

  /** Shows action only for datasets (sequential and pds), for uss files and for uss directories. */
  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val node = selected.getOrNull(0)?.node
    e.presentation.isVisible = selected.size == 1
      && (node is UssFileNode || node is FileLikeDatasetNode || node is LibraryNode || node is UssDirNode)
  }
}
