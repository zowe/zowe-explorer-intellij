/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.actions.workingset

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.containers.isEmpty
import org.zowe.explorer.common.message
import org.zowe.explorer.utils.addTooltip
import org.zowe.explorer.v3.components.files.FilesExplorerComponent
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService

// TODO: doc
class CreateFilesWorkingSetAction : CreateWorkingSetAction() {
  @OptIn(StableStorage::class)
  override fun update(e: AnActionEvent) {
    e.presentation.text = "Files Working Set"
    e.presentation.isEnabledAndVisible = e.place.contains(FilesExplorerComponent.FILES_EXPLORER_COMPONENT_NAME)
    val isConnectionConfigCreated = !StorageService.getService()
      .getConfigsFromStorage(ConfigType.HTTP_CONNECTION_CONFIG_V1)
      .isEmpty()
    e.presentation.isEnabled = isConnectionConfigCreated
    if (!isConnectionConfigCreated) {
      e.presentation.addTooltip(message("create.connection.tooltip"))
    }
  }

  /** Shows add Working Set dialog (for files or for jobs) */
  override fun actionPerformed(e: AnActionEvent) {
    TODO("Not yet implemented")
    // TODO: CreateFilesWorkingSetDialog
  }
}
