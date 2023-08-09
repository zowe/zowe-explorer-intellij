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

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.IconUtil
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import org.zowe.explorer.explorer.ui.FilesWorkingSetNode
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.explorer.ui.getExplorerView

/**
 * Class that represents dataset allocation action with parameters, defined by a user
 */
class AllocateDatasetAction : AllocateActionBase() {

  /**
   * Called when allocate option is chosen from context menu,
   * runs allocate dataset operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    doAllocateAction(e)
  }

  /**
   * Determines if dataset allocation is possible for chosen object.
   * Shows the action if:
   * 1. It is a [FileExplorerView]
   * 2. The first selected node is [FilesWorkingSetNode], [DSMaskNode], [LibraryNode] or [FileLikeDatasetNode]
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodesData = view.mySelectedNodesData
    val node = selectedNodesData.getOrNull(0)?.node
    if (node !is FilesWorkingSetNode && node !is DSMaskNode && node !is LibraryNode && node !is FileLikeDatasetNode) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = true
    e.presentation.icon = IconUtil.addText(AllIcons.FileTypes.Any_type, "DS")
  }

}
