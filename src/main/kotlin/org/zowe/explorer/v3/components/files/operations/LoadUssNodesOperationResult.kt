/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.components.files.operations

import org.zowe.explorer.v3.newoperations.LoadNodesOperationResult
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

// TODO: doc
data class LoadUssNodesOperationResult(override val loadedNodes: List<ExplorerTreeNode>) : LoadNodesOperationResult
