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
import eu.ibagroup.formainframe.explorer.ExplorerUnit

/**
 * Additional class to the ExplorerTreeNode to indicate explorer unit
 * @see ExplorerTreeNode
 */
abstract class ExplorerUnitTreeNodeBase<Value : Any, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<*>,
  val unit: U,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerTreeNode<Value>(value, project, parent, unit.explorer, treeStructure)