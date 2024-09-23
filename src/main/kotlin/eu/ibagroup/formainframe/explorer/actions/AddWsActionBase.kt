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
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.IdeFocusManager
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Abstract action for adding Working Set (for files or for jobs) through UI.
 */
abstract class AddWsActionBase : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /** Shows add Working Set dialog (for files or for jobs) */
  override fun actionPerformed(e: AnActionEvent) {
    service<IdeFocusManager>()
      .runOnOwnContext(DataContext.EMPTY_CONTEXT) {
        val dialog = createDialog(ConfigService.getService().crudable)
        if (dialog.showAndGet()) {
          val state = dialog.state
          val workingSetConfig = state.workingSetConfig
          ConfigService.getService().crudable.add(workingSetConfig)
        }
      }
  }

  /** Presentation text in explorer context menu */
  abstract val presentationTextInExplorer: String

  /** Default presentation text. */
  abstract val defaultPresentationText: String

  /**
   * Implementation should create working set dialog for specific working set type.
   * @param configCrudable crudable from ConfigService.
   * @see Crudable
   * @see ConfigService
   */
  abstract fun createDialog(configCrudable: Crudable): AbstractWsDialog<*, *, *, out AbstractWsDialogState<out WorkingSetConfig, *>>

  override fun isDumbAware(): Boolean {
    return true
  }

  /** Updates text regarding the context from which action should be triggered. */
  override fun update(e: AnActionEvent) {
    e.presentation.text = presentationTextInExplorer
  }
}
