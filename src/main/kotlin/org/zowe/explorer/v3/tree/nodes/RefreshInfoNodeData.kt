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

package org.zowe.explorer.v3.tree.nodes

import javax.swing.Icon

// TODO: doc
open class RefreshInfoNodeData(
  displayName: String,
  tooltip: String,
  icon: Icon,
  var refreshInfo: String = ""
) : ExplorerTreeNodeData(displayName, tooltip, icon)
