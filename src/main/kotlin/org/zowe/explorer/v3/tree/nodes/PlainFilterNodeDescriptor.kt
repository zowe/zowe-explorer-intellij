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

import javax.swing.Icon

/**
 * Node descriptor for the plain filter node. This type of node fetches and carries other related nodes.
 * The difference between [FetcherNodeDescriptor] and [PlainFilterNodeDescriptor] is that the plain filter node
 * does not represent a real node, existing on mainframe
 * @property displayName the name of the node to be displayed (contains filtering information)
 */
abstract class PlainFilterNodeDescriptor(
  displayName: String,
  basePath: List<String>,
  tooltip: String,
  icon: Icon,
  connectionProfile: String
) : FetcherNodeDescriptor(
  displayName,
  basePath,
  tooltip,
  icon,
  connectionProfile
) {
  enum class FilterState {
    INIT,
    BUSY,
    LOADED,
    ERROR
  }

  override val fetchFilter = displayName

  @Volatile
  var filterState = FilterState.INIT
  @Volatile
  var wasLoadedBefore = false
  @Volatile
  var filterError = ""

  /**
   * Check if the provided [elemName] matches the filter.
   * Is needed to identify if some node is related to some other node as a child.
   * E.g.: if there are multiple filters that could point to identical items,
   * then this function defines if the checked node is actually relates to the filter node where the check is triggered
   * @param elemName the element name to check
   * @return true if the element name matches the filter, false otherwise
   */
  abstract fun checkMatchesFilter(elemName: String): Boolean
}