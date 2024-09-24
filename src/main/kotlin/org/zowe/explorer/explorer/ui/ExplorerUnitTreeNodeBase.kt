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

import com.intellij.openapi.project.Project
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.explorer.ExplorerUnit

/**
 * Additional class to the ExplorerTreeNode to indicate explorer unit
 * @see ExplorerTreeNode
 */
abstract class ExplorerUnitTreeNodeBase<Connection: ConnectionConfigBase, Value : Any, U : ExplorerUnit<Connection>>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<Connection, *>,
  val unit: U,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerTreeNode<Connection, Value>(value, project, parent, unit.explorer, treeStructure)
