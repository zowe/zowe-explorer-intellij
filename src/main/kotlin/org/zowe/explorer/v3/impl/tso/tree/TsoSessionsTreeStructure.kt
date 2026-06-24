/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.tso.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.tree.ExplorerTreeStructure
import org.zowe.explorer.v3.tree.nodes.RootNode

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
//        registerProfileNode(
//          ExplorerTreeNode(
//            FilesProfileNodeDescriptor(config.name, config),
//            project,
//            rootNode
//          )
//        )
//      }
  }
}
