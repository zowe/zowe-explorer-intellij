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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialog
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsDialogState
import eu.ibagroup.formainframe.config.ws.ui.JesWorkingSetDialogState
import eu.ibagroup.formainframe.config.ws.ui.initEmptyUuids
import eu.ibagroup.formainframe.config.ws.ui.jes.JesWsDialog
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_CONTEXT_MENU
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll

/**
 * Implementation of AddWsActionBase for JES working sets.
 * @see AddWsActionBase
 * @author Valiantsin Krus
 */
class AddJesWorkingSetAction : AddWsActionBase() {
  override val presentationTextInExplorer = "JES Working Set"
  override val defaultPresentationText = "Create JES Working Set"

  /**
   * Creates dialog for JES Working Set.
   * @see AddWsActionBase.createDialog
   */
  override fun createDialog(configCrudable: Crudable): AbstractWsDialog<*, *, *, out AbstractWsDialogState<out WorkingSetConfig, *>> {
    return JesWsDialog(configCrudable, JesWorkingSetDialogState().initEmptyUuids(configCrudable))
  }

  /**
   * Does the same as the super method but disable action for Files Explorer.
   * @see AddWsActionBase.update
   */
  override fun update(e: AnActionEvent) {
    super.update(e)
    if (
      ConfigService.getService().crudable
        .getAll<ConnectionConfig>()
        .isEmpty()
    ) {
      e.presentation.isEnabled = false
    }
    if (e.place.contains(FILE_EXPLORER_CONTEXT_MENU)) {
      e.presentation.isEnabledAndVisible = false
    }
  }
}
