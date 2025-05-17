/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsDialog
import org.zowe.explorer.config.ws.ui.jes.JesWsDialog
import org.zowe.explorer.config.ws.ui.jes.toDialogState
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JesWsNode
import org.zowe.explorer.utils.crudable.Crudable

/** Action class for the Edit JES working set action */
class EditJesWorkingSetAction : EditWorkingSetAction<JesExplorerView, JesWsNode, JesWorkingSetConfig>() {
  override val explorerViewClass = JesExplorerView::class.java
  override val workingSetNodeClass = JesWsNode::class.java
  override val workingSetConfigClass = JesWorkingSetConfig::class.java

  override fun dialogCreator(
    crudable: Crudable,
    workingSetConfig: JesWorkingSetConfig
  ): AbstractWsDialog<ConnectionConfig, JesWorkingSetConfig, *, *> {
    return JesWsDialog(
      crudable,
      workingSetConfig.toDialogState().apply { mode = DialogMode.UPDATE }
    )
  }
}
