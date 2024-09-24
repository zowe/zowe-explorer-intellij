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

package org.zowe.explorer.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.explorer.JesWorkingSet

/** JES working set node representation */
class JesWsNode(
  workingSet: JesWorkingSet,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  treeStructure: ExplorerTreeStructureBase
) : WorkingSetNode<ConnectionConfig, JobsFilter>(
  workingSet, project, parent, treeStructure
), RefreshableNode {

  override val regularTooltip = "JES Working Set"

  override fun update(presentation: PresentationData) {
    presentation.addText(value.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    when {
      value.connectionConfig == null -> connectionIsNotSet(presentation)
      value.masks.isEmpty() -> destinationsAreEmpty(presentation)
      else -> regular(presentation)
    }
    if (treeStructure.showWorkingSetInfo) {
      addInfo(presentation)
    }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.masks.map { JesFilterNode(it, notNullProject, this, value as JesWorkingSet, treeStructure) }
      .toMutableSmartList().also { cachedChildrenInternal = it }
  }
}
