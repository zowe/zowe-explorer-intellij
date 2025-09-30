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

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.util.ui.tree.AbstractTreeModel
import org.zowe.explorer.v3.tree.ExplorerTreeView

// TODO: doc
class FilesExplorerTreeView(
  explorerName: String,
  explorerTreeModel: AbstractTreeModel
) : ExplorerTreeView(explorerName, explorerTreeModel) {
  override val actionGroup =
    ActionManager.getInstance()
      .getAction("org.zowe.explorer.components.files.FilesExplorerActionBarGroup") as ActionGroup
  override val contextMenuGroup =
    ActionManager.getInstance()
      .getAction("org.zowe.explorer.components.files.FilesExplorerContextMenuGroup") as ActionGroup

  override fun initActionToolbar(): ActionToolbar {
    return ActionManager.getInstance()
      .createActionToolbar(explorerName, actionGroup, true)
  }

  override fun dispose() {
    // TODO: implement if needed
  }
}
