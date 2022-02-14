/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.config.ws.ui.*
import org.zowe.explorer.config.ws.ui.jobs.JobsWsDialog
import org.zowe.explorer.explorer.ui.FILE_EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.JES_EXPLORER_VIEW
import org.zowe.explorer.utils.crudable.Crudable

class AddJobsWorkingSetAction : AddWsActionBase() {
  override val explorerView = JES_EXPLORER_VIEW
  override val presentationTextInExplorer = "Jobs Working Set"
  override val defaultPresentationText = "Create Jobs Working Set"

  override fun createDialog(configCrudable: Crudable): AbstractWsDialog<*, *, out AbstractWsDialogState<out WorkingSetConfig, *>> {
    return JobsWsDialog(configCrudable, JobsWorkingSetDialogState().initEmptyUuids(configCrudable))
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.getData(FILE_EXPLORER_VIEW) != null) {
      e.presentation.isEnabledAndVisible = false
    }
  }
}
