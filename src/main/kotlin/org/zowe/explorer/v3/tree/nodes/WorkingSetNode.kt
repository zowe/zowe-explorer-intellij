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

// TODO: doc
abstract class WorkingSetNode(
  project: Project,
  nodeData: RefreshInfoNodeData,
  parent: ExplorerTreeRootNode,
) : ExpandableNode(project, nodeData, parent) {
  override fun isAlwaysExpand() = true

  override fun update(presentation: PresentationData) {
    presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.tooltip = nodeData.tooltip
    // TODO: finalize
    //    when {
    //      valueForFilesWS.connectionConfig == null -> connectionIsNotSet(presentation)
    //      valueForFilesWS.masks.isEmpty() && valueForFilesWS.ussPaths.isEmpty() -> destinationsAreEmpty(presentation)
    //      else -> regular(presentation)
    //    }
    //    if (treeStructure.showWorkingSetInfo) {
    //      addInfo(presentation)
    //    }
  }
}