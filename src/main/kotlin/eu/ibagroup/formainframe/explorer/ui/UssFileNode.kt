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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.ui.AnimatedIcon
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/** USS file representation in the explorer tree */
class UssFileNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  unit: ExplorerUnit<ConnectionConfig>,
  treeStructure: ExplorerTreeStructureBase,
  override val currentSortQueryKeysList: List<SortQueryKeys> = mutableListOf(),
  override val sortedNodes: List<AbstractTreeNode<*>> = mutableListOf()
) : ExplorerUnitTreeNodeBase<ConnectionConfig, MFVirtualFile, ExplorerUnit<ConnectionConfig>>(
  file, project, parent, unit, treeStructure
), UssNode {

  override fun update(presentation: PresentationData) {
    updateNodeTitleUsingCutBuffer(value.presentableName, presentation)
    val icon = IconUtil.computeFileIcon(value, Iconable.ICON_FLAG_READ_STATUS, explorer.nullableProject)
    if (this.navigating) {
      presentation.setIcon(AnimatedIcon.Default())
    } else {
      presentation.setIcon(icon)
    }
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return mutableListOf()
  }
}
