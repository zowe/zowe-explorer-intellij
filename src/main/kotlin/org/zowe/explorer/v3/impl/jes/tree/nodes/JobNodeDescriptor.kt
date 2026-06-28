/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.tree.nodes

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.impl.connection.ConnectionProfileRelated
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.Traversable

/**
 * Node descriptor for a JES job.
 * @param elemName the job name and ID shown in the tree
 * @param placingPath the path segments for placing in the virtual file system
 * @param connectionProfile the connection profile path from the Zowe Team Config
 */
class JobNodeDescriptor(
  override val elemName: String,
  override val placingPath: List<String>,
  override val connectionProfile: String
) : ExplorerTreeNodeDescriptor(
  elemName,
  "JES job",
  ZoweExplorerIcons.jesJob
), ConnectionProfileRelated, Traversable, JesExplorerRelated
