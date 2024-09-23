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
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.explorer.FilesWorkingSet

/** File Explorer working set representation */
class FilesWorkingSetNode(
  workingSet: FilesWorkingSet,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  treeStructure: ExplorerTreeStructureBase
) : WorkingSetNode<ConnectionConfig, DSMask>(
  workingSet, project, parent, treeStructure
), RefreshableNode {

  private val valueForFilesWS = value as FilesWorkingSet

  override val regularTooltip = "Files Working Set"

  override fun update(presentation: PresentationData) {
    presentation.addText(valueForFilesWS.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    when {
      valueForFilesWS.connectionConfig == null -> connectionIsNotSet(presentation)
      valueForFilesWS.masks.isEmpty() && valueForFilesWS.ussPaths.isEmpty() -> destinationsAreEmpty(presentation)
      else -> regular(presentation)
    }
    if (treeStructure.showWorkingSetInfo) {
      addInfo(presentation)
    }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return valueForFilesWS.masks.map { DSMaskNode(it, notNullProject, this, valueForFilesWS, treeStructure) }.plus(
      valueForFilesWS.ussPaths.map {
        UssDirNode(
          it,
          notNullProject,
          this,
          valueForFilesWS,
          treeStructure,
          isRootNode = true
        )
      }
    ).toMutableList().also { cachedChildrenInternal = it }
  }
}
