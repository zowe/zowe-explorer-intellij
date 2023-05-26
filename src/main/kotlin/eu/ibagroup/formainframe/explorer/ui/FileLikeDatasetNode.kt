/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import icons.ForMainframeIcons

private val migratedIcon = AllIcons.FileTypes.Any_type

/** Datasets and USS files representation as file node in the explorer tree view */
class FileLikeDatasetNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  unit: ExplorerUnit,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<MFVirtualFile, ExplorerUnit>(
  file, project, parent, unit, treeStructure
), MFNode {

  override fun isAlwaysLeaf(): Boolean {
    return !value.isDirectory
  }

  override fun update(presentation: PresentationData) {
    val attributes = service<DataOpsManager>().tryToGetAttributes(value)
    when (attributes) {
      is RemoteDatasetAttributes -> {
        if (this.navigating) {
          presentation.setIcon(AnimatedIcon.Default())
        } else {
          presentation.setIcon(
            if (value.isDirectory) ForMainframeIcons.DatasetMask else if (attributes.isMigrated) migratedIcon else IconUtil.addText(
              AllIcons.FileTypes.Any_type,
              "DS"
            )
          )
        }

      }

      is RemoteMemberAttributes -> {
        if (this.navigating) {
          presentation.setIcon(AnimatedIcon.Default())
        } else {
          presentation.setIcon(ForMainframeIcons.MemberIcon)
        }
      }

      else -> {
        if (this.navigating) {
          presentation.setIcon(AnimatedIcon.Default())
        } else {
          presentation.setIcon(AllIcons.FileTypes.Any_type)
        }
      }
    }
    updateMainTitleUsingCutBuffer(value.presentableName, presentation)
    val volser = explorer.componentManager.service<DataOpsManager>()
      .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
      .getAttributes(value)?.volser
    volser?.let { presentation.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.cachedChildren
      .map { FileLikeDatasetNode(value, notNullProject, this, unit, treeStructure) }.toMutableSmartList()
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }
}
