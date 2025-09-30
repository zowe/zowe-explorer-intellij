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
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes

// TODO: doc
class LoadingNode(
  project: Project,
  tooltip: String = "loading...",
  parent: ExplorerTreeNode
) : LeafNode(
  project,
  ExplorerTreeNodeData("loading...", tooltip),
  parent
) {
  override fun update(presentation: PresentationData) {
    presentation.tooltip = nodeData.tooltip
    presentation.addText(name, SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}
