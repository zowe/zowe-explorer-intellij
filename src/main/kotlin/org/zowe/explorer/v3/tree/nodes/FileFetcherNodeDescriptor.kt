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

import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import javax.swing.Icon

// TODO: doc
open class FileFetcherNodeDescriptor(
  displayName: String,
  tooltip: String,
  icon: Icon,
  var fetchedFilesCount: Int = 0,
  override var connectionConfigUuid: String
) : ExplorerTreeNodeDescriptor(displayName, tooltip, icon, isLeaf=false, hasExpandChevron=true),
  ConnectionConfigRelated,
  Traversable
{
  override val path: List<String>
    get() = TODO("Not yet implemented")
}
