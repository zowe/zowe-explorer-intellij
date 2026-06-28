/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.actions.CreateProfileAction
import org.zowe.explorer.v3.impl.files.dialogs.CreateFilesProfileDialog
import org.zowe.explorer.v3.impl.files.tree.FilesExplorerComponent
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService

class CreateFilesProfileAction : CreateProfileAction(
  "Files Profile",
  FilesExplorerComponent.FILES_EXPLORER_COMPONENT_NAME
) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)
    CreateFilesProfileDialog(project, configType).showAndGet()
  }
}