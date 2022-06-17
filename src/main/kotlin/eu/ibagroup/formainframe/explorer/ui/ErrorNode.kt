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

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.explorer.Explorer

class ErrorNode(
  project: Project,
  parent: ExplorerTreeNode<*>,
  explorer: Explorer<*>,
  treeStructure: ExplorerTreeStructureBase,
  override var text: String = message("title.error")
) : InfoNodeBase(project, parent, explorer, treeStructure) {


  override val textAttributes: SimpleTextAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES

}
