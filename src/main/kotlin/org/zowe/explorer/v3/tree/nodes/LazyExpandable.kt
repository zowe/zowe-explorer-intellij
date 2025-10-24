/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

/**
 * Lazy expandable interface for nodes that can carry child elements that are to be loaded
 * only when the node is expanded
 * @property wasExpanded indicates whether the node was already expanded or not
 */
interface LazyExpandable {
  var wasExpanded: Boolean

  /**
   * Expand the node. The function is designed to start children loading if they are not loaded yet
   * @param node the node to expand and load children for
   */
  fun expandNode(node: ExplorerTreeNode)
}