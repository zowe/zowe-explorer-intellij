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

package org.zowe.explorer.v3.components.files

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.LeafNode

// TODO: doc
class UssFileNode(
  project: Project,
  nodeData: UssFileNodeData,
  parent: ExplorerTreeNode
) : LeafNode(project, nodeData, parent) {
  override fun update(presentation: PresentationData) {
    presentation.tooltip = nodeData.tooltip
    presentation.addText(nodeData.displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
  }
}
