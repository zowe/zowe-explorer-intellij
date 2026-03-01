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

package org.zowe.explorer.v3.components.tso

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.components.files.FilesWorkingSetNodeDescriptor
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.tree.ExplorerTreeStructure
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.RootNode
import kotlin.collections.forEach

// TODO: doc
class TsoSessionsTreeStructure(private val project: Project) : ExplorerTreeStructure(project) {
  override val rootNode by lazy { RootNode(project) }

  fun addTsoProfilesFromConfigs() {
    TODO("Not yet implemented")
    // TODO: implement when needed
//    ConfigCacheService.getService()
//      .getConfigsFromCache(ConfigType.TSO_SESSION_CONFIG_V1)
//      .toList()
//      .forEach { config ->
//        config as TsoProfileConfig
//        registerWorkingSetNode(
//          ExplorerTreeNode(
//            FilesWorkingSetNodeDescriptor(config.name, config),
//            project,
//            rootNode
//          )
//        )
//      }
  }
}
