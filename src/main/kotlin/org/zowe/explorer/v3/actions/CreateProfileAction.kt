/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.utils.addTooltip
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

abstract class CreateProfileAction(
  private val profileType: String,
  private val targetExplorer: String
) : DumbAwareEDTAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.text = profileType
    e.presentation.isEnabledAndVisible = e.place.contains(targetExplorer)
    val project = e.project ?: return
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)
    val configFile = ZoweConfigService.getService().resolveConfigFile(configType, project.basePath)
    if (!configFile.exists()) {
      e.presentation.isEnabled = false
      e.presentation.addTooltip("Create Zowe Team Config first or select other config type")
    }
  }
}