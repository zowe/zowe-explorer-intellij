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

import com.intellij.ide.IdeBundle.message
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.explorer.Explorer

/** Node that is displayed when no content is loaded yet */
class LoadingNode(
  project: Project,
  parent: ExplorerTreeNode<*>,
  explorer: Explorer<*>,
  treeStructure: ExplorerTreeStructureBase
) : InfoNodeBase(project, parent, explorer, treeStructure) {

  override val text: String = message("treenode.loading")

  override val textAttributes: SimpleTextAttributes = SimpleTextAttributes.GRAYED_ATTRIBUTES

}
