/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.teamconfig.dialogs.CreateTeamConfigDialog

/**
 * IntelliJ action that opens the [CreateTeamConfigDialog] to generate a new
 * Zowe Team Config file.
 */
class CreateTeamConfigAction : DumbAwareEDTAction() {
  override fun actionPerformed(e: AnActionEvent) {
    CreateTeamConfigDialog(e.project).showAndGet()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.text = "Zowe Team Config"
    e.presentation.description = "Create new Zowe team configuration file"
  }
}