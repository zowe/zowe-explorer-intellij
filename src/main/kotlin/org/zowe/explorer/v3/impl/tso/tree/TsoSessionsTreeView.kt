/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.tso.tree

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.util.ui.tree.AbstractTreeModel
import org.zowe.explorer.v3.tree.ExplorerTreeView

// TODO: doc
class TsoSessionsTreeView(
  explorerName: String,
  explorerTreeModel: AbstractTreeModel
) : ExplorerTreeView(explorerName, explorerTreeModel) {
  override val leftActionGroup =
    ActionManager.getInstance()
      .getAction("org.zowe.explorer.components.tso.TsoSessionsLeftActionBarGroup") as ActionGroup
  override val rightActionGroup =
    ActionManager.getInstance()
      .getAction("org.zowe.explorer.components.tso.TsoSessionsRightActionBarGroup") as ActionGroup
  override val contextMenuGroup =
    ActionManager.getInstance()
      .getAction("org.zowe.explorer.components.tso.TsoSessionsContextMenuGroup") as ActionGroup

  override fun dispose() {
    // TODO: implement if needed
  }
}
