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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.JesWorkingSetImpl

/** JES Explorer root node, that is hidden, but aggregates all nodes in JES Explorer. */
class JesExplorerRootNode(
  value: Explorer<ConnectionConfig, *>, project: Project,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerTreeNode<ConnectionConfig, Explorer<ConnectionConfig, *>>(
  value, project,
  null,
  value, treeStructure
) {
  override fun update(presentation: PresentationData) {

  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return explorer.units.filterIsInstance<JesWorkingSetImpl>().map {
      JesWsNode(it, notNullProject, this, treeStructure)
    }.toMutableList()
  }
}
