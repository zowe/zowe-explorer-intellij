/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.content.ContentManagerUtil
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionDialog
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.zosmf.initEmptyUuids
import eu.ibagroup.formainframe.explorer.hints.Hint
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.castOrNull
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

/**
 * Action for adding Connection through UI.
 */
class AddConnectionAction : AnAction() {

  /** Shows connection dialog */
  override fun actionPerformed(e: AnActionEvent) {
    val state = ConnectionDialog.showAndTestConnection(
      crudable = configCrudable,
      project = e.project,
      initialState = ConnectionDialogState().initEmptyUuids(configCrudable)
    )
    if (state != null) {
      val connectionConfig = state.connectionConfig
      CredentialService.instance.setCredentials(connectionConfig.uuid, state.username, state.password)
      configCrudable.add(connectionConfig)
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
    val contentManager = ContentManagerUtil.getContentManagerFromContext(e.dataContext, true)
    val selectedContent = contentManager?.selectedContent
    val toolbar = (selectedContent?.component.castOrNull<SimpleToolWindowPanel>())?.toolbar
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