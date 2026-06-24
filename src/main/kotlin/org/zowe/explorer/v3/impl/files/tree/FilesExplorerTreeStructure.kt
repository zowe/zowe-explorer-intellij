/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.files.tree.nodes.FilesProfileNodeDescriptor
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.tree.ExplorerTreeStructure
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.RootNode
import kotlin.collections.forEach

// TODO: doc
class FilesExplorerTreeStructure(private val project: Project) : ExplorerTreeStructure(project) {
  override val rootNode by lazy { RootNode(project) }

  fun addFilesProfilesFromConfigs() {
    // TODO: check that the profile node is not already initialized (by uuid)
    // TODO: pathStrings forming logic
    ConfigCacheService.getService()
      .getConfigsFromCache(ConfigType.FILES_WORKING_SET_CONFIG_V1)
      .toList()
      .forEach { config ->
        config as FilesWorkingSetConfig
        registerProfileNode(
          ExplorerTreeNode(
            FilesProfileNodeDescriptor(config.name, config),
            project,
            rootNode
          )
        )
      }
  }
}
