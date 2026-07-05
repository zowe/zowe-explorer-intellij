/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.tree.ExplorerTreeComponent

/**
 * Files Explorer component that displays `files_ij` profiles
 * from the `explorer_ij` section of the active Zowe Team Config.
 * Automatically syncs profiles on any config file edit or programmatic write
 */
class FilesExplorerComponent(project: Project) : ExplorerTreeComponent(project) {
  companion object {
    const val FILES_EXPLORER_COMPONENT_NAME = "Files Explorer"
  }

  override val explorerName = FILES_EXPLORER_COMPONENT_NAME
  override val explorerTreeStructure = FilesExplorerTreeStructure(project)
  override val explorerTreeView = FilesExplorerTreeView(explorerName, explorerAsyncTreeModel)
}