/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.utils.service // TODO: remove in v1.*.*-223 and greater
import org.zowe.explorer.vfs.MFVirtualFile
import icons.ForMainframeIcons

private val migratedIcon = AllIcons.FileTypes.Any_type

/** PS dataset or a PDS dataset member representation as file node in the explorer tree view */
class FileLikeDatasetNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  unit: ExplorerUnit<ConnectionConfig>,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<ConnectionConfig, MFVirtualFile, ExplorerUnit<ConnectionConfig>>(
  file, project, parent, unit, treeStructure
) {

  override fun isAlwaysLeaf(): Boolean {
    return !value.isDirectory
  }

  override fun update(presentation: PresentationData) {
    when (val attributes = service<DataOpsManager>().tryToGetAttributes(value)) {
      is RemoteDatasetAttributes -> {
        if (this.navigating) {
          presentation.setIcon(AnimatedIcon.Default())
        } else {
          presentation.apply {
            setIcon(
              if (value.isDirectory) ForMainframeIcons.DatasetMask else if (attributes.isMigrated) migratedIcon else IconUtil.addText(
                AllIcons.FileTypes.Any_type,
                "DS"
              )
            )
            if (attributes.isMigrated) forcedTextForeground = JBColor.GRAY
          }
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
    updateNodeTitleUsingCutBuffer(value.presentableName, presentation)
    val dataOpsManager = explorer.componentManager.service<DataOpsManager>()
    getVolserIfPresent(dataOpsManager, value)
      ?.let { presentation.addText(it, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.cachedChildren
      .map { FileLikeDatasetNode(value, notNullProject, this, unit, treeStructure) }.toMutableSmartList()
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }
}
