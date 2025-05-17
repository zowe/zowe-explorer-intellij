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
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsDialog
import org.zowe.explorer.config.ws.ui.files.FilesWorkingSetDialog
import org.zowe.explorer.config.ws.ui.files.toDialogState
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FilesWorkingSetNode
import org.zowe.explorer.utils.crudable.Crudable

/** Action class for the Edit working set action */
class EditFilesWorkingSetAction : EditWorkingSetAction<FileExplorerView, FilesWorkingSetNode, FilesWorkingSetConfig>() {
  override val explorerViewClass = FileExplorerView::class.java
  override val workingSetNodeClass = FilesWorkingSetNode::class.java
  override val workingSetConfigClass = FilesWorkingSetConfig::class.java

  override fun dialogCreator(
    crudable: Crudable,
    workingSetConfig: FilesWorkingSetConfig
  ): AbstractWsDialog<ConnectionConfig, FilesWorkingSetConfig, *, *> {
    return FilesWorkingSetDialog(
      crudable,
      workingSetConfig.toDialogState().apply { mode = DialogMode.UPDATE }
    )
  }
}
