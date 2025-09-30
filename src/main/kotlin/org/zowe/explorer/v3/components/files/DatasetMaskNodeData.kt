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

package org.zowe.explorer.v3.components.files

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import org.zowe.explorer.v3.tree.nodes.RefreshInfoNodeData

// TODO: doc
class DatasetMaskNodeData(
  datasetMask: String,
  tooltip: String = "Data set mask",
  override val connectionConfigUuid: String
) : RefreshInfoNodeData(datasetMask, tooltip, ZoweExplorerIcons.datasetMask),
  ConnectionConfigRelated
