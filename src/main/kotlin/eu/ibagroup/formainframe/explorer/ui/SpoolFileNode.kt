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
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.vfs.MFVirtualFile

private val spoolFileIcon = AllIcons.FileTypes.Text

/** Representation of a spool file in JES Explorer */
class SpoolFileNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  unit: ExplorerUnit,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<MFVirtualFile, ExplorerUnit>(
  file, project, parent, unit, treeStructure
), MFNode {
  override fun update(presentation: PresentationData) {
    val attributes = service<DataOpsManager>().tryToGetAttributes(value) as? RemoteSpoolFileAttributes
    val spoolFile = attributes?.info
    if (this.navigating) {
      presentation.setIcon(AnimatedIcon.Default())
    } else {
      presentation.setIcon(spoolFileIcon)
    }
    presentation.addText("${value.name} ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.addText(spoolFile?.procStep ?: "", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return mutableListOf()
  }

  override fun getVirtualFile(): MFVirtualFile? {
    return value
  }

  init {
    value?.isWritable = false
  }
}
