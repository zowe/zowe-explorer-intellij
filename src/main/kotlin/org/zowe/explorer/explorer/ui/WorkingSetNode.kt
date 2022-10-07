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
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import org.zowe.explorer.config.connect.username
import org.zowe.explorer.explorer.WorkingSet

private val regularIcon = AllIcons.Nodes.Project
private val errorIconElement = AllIcons.Nodes.ErrorMark
private val grayscaleIcon = IconUtil.desaturate(regularIcon)
private val errorIcon = LayeredIcon(grayscaleIcon, errorIconElement)

/** Base implementation of working set tree node */
abstract class WorkingSetNode<MaskType>(
  workingSet: WorkingSet<MaskType>,
  project: Project,
  parent: ExplorerTreeNode<*>,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<WorkingSet<MaskType>, WorkingSet<MaskType>>(
  workingSet, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {
  protected var cachedChildrenInternal: MutableCollection<out AbstractTreeNode<*>>? = null

  val cachedChildren: MutableCollection<out AbstractTreeNode<*>>
    get() = cachedChildrenInternal ?: mutableListOf()

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  /**
   * Set up default representation of a working set
   * @param presentation the presentation, which icon and tooltip will be assigned to
   */
  protected fun regular(presentation: PresentationData) {
    presentation.setIcon(regularIcon)
    presentation.tooltip = "Working set"
  }

  /**
   * Set up the presentation when there is no connection set
   * @param presentation the presentation, which icon, text and tooltip will be assigned to
   */
  protected fun connectionIsNotSet(presentation: PresentationData) {
    presentation.setIcon(errorIcon)
    presentation.addText(" ", SimpleTextAttributes.ERROR_ATTRIBUTES)
    presentation.addText("Error: Check connection", SimpleTextAttributes.ERROR_ATTRIBUTES)
    presentation.tooltip = "Check connection for this working set"
  }

  /**
   * Set up the presentation for the empty working set
   * @param presentation the presentation, which icon and tooltip will be assigned to
   */
  protected fun destinationsAreEmpty(presentation: PresentationData) {
    presentation.setIcon(grayscaleIcon)
    presentation.tooltip = "Empty working set"
  }

  /**
   * Add info about the connection of the working set
   * @param presentation the presentation, which explanatory text will be assigned to
   */
  protected fun addInfo(presentation: PresentationData) {
    val connectionConfig = value.connectionConfig ?: return
    val url = value.connectionConfig?.url ?: return
    val username = username(connectionConfig)
    val formedUsername = if (connectionConfig.zoweConfigPath == null) username else "*".repeat(username.length)
    presentation.addText(" $formedUsername on ${connectionConfig.name} [${url}]", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}
