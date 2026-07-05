/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.tree.ExplorerTreeComponent

/**
 * JES Explorer component that displays `jes_ij` profiles
 * from the `explorer_ij` section of the active Zowe Team Config.
 * Automatically syncs profiles on any config file edit or programmatic write
 */
class JesExplorerComponent(private val project: Project) : ExplorerTreeComponent(project) {
  companion object {
    const val JES_EXPLORER_COMPONENT_NAME = "JES Explorer"
  }

  override val explorerName = JES_EXPLORER_COMPONENT_NAME
  override val explorerTreeStructure = JesExplorerTreeStructure(project)
  override val explorerTreeView = JesExplorerTreeView(explorerName, explorerAsyncTreeModel)
}