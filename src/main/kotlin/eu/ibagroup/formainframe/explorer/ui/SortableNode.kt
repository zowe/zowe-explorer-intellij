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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys

/**
 * Interface which represents any sortable Node
 */
interface SortableNode {
  /**
   * Field which holds and identifies the current sort keys for particular Node
   */
  val currentSortQueryKeysList : List<SortQueryKeys>

  /**
   * Stores the sorted nodes for particular SortableNode
   */
  val sortedNodes: List<AbstractTreeNode<*>>

  /**
   * Method sorts the children nodes regarding the sort keys are currently enabled
   * @param sortKeys - Sort keys to check
   * @return list of sorted children nodes
   */
  fun <Node : AbstractTreeNode<*>> sortChildrenNodes(childrenNodes: List<Node>, sortKeys: List<SortQueryKeys>): List<Node> = mutableListOf()

}
