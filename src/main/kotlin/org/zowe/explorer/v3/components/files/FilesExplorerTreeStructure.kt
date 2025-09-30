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

package org.zowe.explorer.v3.components.files

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeRootNode
import org.zowe.explorer.v3.tree.ExplorerTreeStructure
import kotlin.collections.forEach

// TODO: doc
class FilesExplorerTreeStructure(private val project: Project) : ExplorerTreeStructure(project) {
  override val rootNode by lazy { ExplorerTreeRootNode(project) }

  @OptIn(StableStorage::class)
  fun addFilesWorkingSetsFromConfigs() {
    // TODO: check that the working set node is not already initialized (by uuid)
    StorageService.getService()
      .getConfigsFromStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)
      .toList()
      .forEach { config ->
        config as FilesWorkingSetConfig
        registerWorkingSetNode(
          FilesWorkingSetNode(
            project,
            FilesWorkingSetNodeData(config.name, config),
            rootElement
          )
        )
      }
  }
}
