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
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.HyperlinkAdapter
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionDialog
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.zosmf.initEmptyUuids
import eu.ibagroup.formainframe.explorer.ACTION_TOOLBAR
import eu.ibagroup.formainframe.explorer.hints.Hint
import eu.ibagroup.formainframe.explorer.ui.EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.utils.castOrNull
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

/**
 * Action for adding Connection through UI.
 */
class AddConnectionAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /** Shows connection dialog */
  override fun actionPerformed(e: AnActionEvent) {
    val state = ConnectionDialog.showAndTestConnection(
      crudable = ConfigService.getService().crudable,
      project = e.project,
      initialState = ConnectionDialogState().initEmptyUuids(ConfigService.getService().crudable)
    )
    if (state != null) {
      val connectionConfig = state.connectionConfig
      CredentialService.getService().setCredentials(connectionConfig.uuid, state.username, state.password)
      ConfigService.getService().crudable.add(connectionConfig)
      showHint(e)
    } else {
      return
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}

/**
 * Shows a hint for adding working sets after the connection has been created.
 * Hint is shown only if the current working set tree is empty.
 * @param e event after which to show hint.
 */
private fun showHint(e: AnActionEvent) {
  val view = e.getData(EXPLORER_VIEW)
  if (view?.myTree?.isEmpty == true) {
    val toolbar = e.getData(ACTION_TOOLBAR)?.component
    val component = if (toolbar?.components?.isNotEmpty() == true) {
      toolbar.components?.get(0).castOrNull<JComponent>()
    } else {
      null
    }
    component?.let {
      val content = when (view) {
        is FileExplorerView -> "z/OS datasets and USS files"
        is JesExplorerView -> "z/OS jobs"
        else -> null
      }
      val text = "Now you can add working set to browse<br> $content.<br>" +
        "<a href\"\">Click here to add...</a>"
      val hyperlinkAction = when (view) {
        is FileExplorerView -> {
          { AddWorkingSetAction().actionPerformed(e) }
        }

        is JesExplorerView -> {
          { AddJesWorkingSetAction().actionPerformed(e) }
        }

        else -> null
      }
      val listener = object : HyperlinkAdapter() {
        override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
          hyperlinkAction?.let { it() }
        }
      }
      Hint(text, hyperlinkListener = listener).show(it, Hint.BOTTOM_MIDDLE)
    }
  }
}
