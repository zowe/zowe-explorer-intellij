/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.vault.tree.nodes

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNodeDescriptor

/**
 * Leaf node descriptor representing a secure credential entry (e.g. "user and password").
 * Displayed with a key icon
 */
class SecureEntryNodeDescriptor(
  entryName: String
) : ExplorerTreeNodeDescriptor(entryName, entryName, ZoweExplorerIcons.keyIcon, isLeaf = true),
  SecureVaultRelated
