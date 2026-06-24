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

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.state.config.WorkingSetConfig

/**
 * Base node descriptor for profile nodes in the explorer tree.
 * Subclasses represent specific profile types (e.g., files, JES) and provide
 * children resolution logic by reading masks/filters from the associated [config].
 *
 * @param displayName the display name shown in the tree
 * @param tooltip the tooltip text shown on hover
 * @param config the profile configuration backing this node, or `null` if not yet loaded
 */
open class ProfileNodeDescriptor(
  displayName: String,
  tooltip: String,
  var config: WorkingSetConfig?
) : ExplorerTreeNodeDescriptor(displayName, tooltip, ZoweExplorerIcons.profileIcon, false)