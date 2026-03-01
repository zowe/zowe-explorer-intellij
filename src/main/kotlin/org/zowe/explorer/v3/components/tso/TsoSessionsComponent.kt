/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.components.tso

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.tree.ExplorerTreeComponent

// TODO: doc
class TsoSessionsComponent(project: Project) : ExplorerTreeComponent() {
  companion object {
    const val TSO_SESSIONS_COMPONENT_NAME = "TSO Sessions"
  }

  override val explorerName = TSO_SESSIONS_COMPONENT_NAME
  override val explorerTreeStructure = TsoSessionsTreeStructure(project)
  override val explorerTreeView = TsoSessionsTreeView(explorerName, explorerAsyncTreeModel)
}
