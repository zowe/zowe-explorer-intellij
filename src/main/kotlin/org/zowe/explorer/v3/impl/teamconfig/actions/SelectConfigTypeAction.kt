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
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

class SelectConfigTypeAction(
  private val configType: ConfigType
) : DumbAwareEDTAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.text = configType.displayName.replaceFirstChar {
      if (it.isLowerCase()) it.titlecase() else it.toString()
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ZoweConfigService.getService().setSelectedConfigType(project, configType)
  }
}