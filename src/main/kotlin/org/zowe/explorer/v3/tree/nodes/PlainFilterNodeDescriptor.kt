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

// TODO: doc
abstract class PlainFilterNodeDescriptor(
  displayName: String,
  basePath: List<String>,
  tooltip: String,
  icon: Icon,
  connectionConfigUuid: String
) : FetcherNodeDescriptor(
  displayName,
  basePath,
  tooltip,
  icon,
  connectionConfigUuid
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
   * Check if the provided [elemName] matches the filter
   * @param elemName the element name to check
   * @return true if the element name matches the filter, false otherwise
   */
  abstract fun checkMatchesFilter(elemName: String): Boolean
}