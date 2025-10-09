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

import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.state.config.WorkingSetConfig

// TODO: doc
open class WorkingSetNodeDescriptor(
  displayName: String,
  tooltip: String,
  var config: WorkingSetConfig?
) : ExplorerTreeNodeDescriptor(displayName, tooltip, ZoweExplorerIcons.workingSetIcon, false)
//  override fun update(presentation: PresentationData) {
//    presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
//    presentation.tooltip = nodeData.tooltip
//
//    // TODO: finalize
//    // Connection is not set:
////    icon = errorIcon
////    presentation.setIcon(errorIcon)
////    presentation.addText(" ", SimpleTextAttributes.ERROR_ATTRIBUTES)
////    presentation.addText("Error: Check connection", SimpleTextAttributes.ERROR_ATTRIBUTES)
////    presentation.tooltip = "Check connection for this working set"
//    //    when {
//    //      valueForFilesWS.connectionConfig == null -> connectionIsNotSet(presentation)
//    //      valueForFilesWS.masks.isEmpty() && valueForFilesWS.ussPaths.isEmpty() -> destinationsAreEmpty(presentation)
//    //      else -> regular(presentation)
//    //    }
//    //    if (treeStructure.showWorkingSetInfo) {
//    //      addInfo(presentation)
//    //    }
//  }
