/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.files.ds.dialogs.MemberPropertiesDialog
import org.zowe.explorer.v3.impl.files.ds.tree.nodes.MemberNodeDescriptor
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService

/**
 * Action to show properties of a data set member.
 * Visible when a single node with a [MemberNodeDescriptor] is selected
 */
class MemberPropertiesAction : DumbAwareEDTAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedNode = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
      .singleOrNull() ?: return
    val descriptor = selectedNode.nodeDescriptor as? MemberNodeDescriptor ?: return
    val memberItem = descriptor.memberItem ?: return
    MemberPropertiesDialog(project, memberItem).showAndGet()
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selectedNodes = ExplorerTreeComponentService.getService()
      .getActiveExplorerComponent(project)
      .selectedNodes
    val descriptor = selectedNodes.singleOrNull()?.nodeDescriptor
    e.presentation.isEnabledAndVisible =
      descriptor is MemberNodeDescriptor && descriptor.memberItem != null
  }
}