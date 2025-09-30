/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// TODO: doc
abstract class RefreshableNode(
  project: Project,
  nodeData: RefreshInfoNodeData,
  parent: ExplorerTreeNode,
) : ExpandableNode(project, nodeData, parent) {
  private fun getCurrentRefreshDateTime(): String {
    return DateTimeFormatter
      .ofPattern("dd MMM YYYY HH:mm:ss", Locale.ENGLISH)
      .withZone(ZoneId.systemDefault())
      .format(LocalDateTime.now())
      .uppercase(Locale.getDefault())
  }

  override fun startChildrenLoading() {
    nodeData as RefreshInfoNodeData
    nodeData.refreshInfo = "last refresh: ${getCurrentRefreshDateTime()}"
    super.startChildrenLoading()
  }

  // TODO: doc
  fun refreshNode() {
    // TODO: invalidate same nodes in other projects as well here???
    startChildrenLoading()
  }

  override fun update(presentation: PresentationData) {
    presentation.tooltip = nodeData.tooltip
    presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    nodeData as RefreshInfoNodeData
    presentation.addText(" ${nodeData.refreshInfo}", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }
}
