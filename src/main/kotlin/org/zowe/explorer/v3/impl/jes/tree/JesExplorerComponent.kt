/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigEventListener
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService
import org.zowe.explorer.v3.tree.ExplorerTreeComponent

// TODO: doc
@OptIn(StableStorage::class)
class JesExplorerComponent(project: Project) : ExplorerTreeComponent() {
  companion object {
    const val JES_EXPLORER_COMPONENT_NAME = "JES Explorer"
  }

  override val explorerName = JES_EXPLORER_COMPONENT_NAME
  override val explorerTreeStructure = JesExplorerTreeStructure(project)
  override val explorerTreeView = JesExplorerTreeView(explorerName, explorerAsyncTreeModel)

  init {
    explorerTreeStructure.addJesProfilesFromConfigs()
    subscribe(
      StorageService.STORAGE_CONFIGS_TOPIC,
      object : ConfigEventListener {
        override fun registered(configType: ConfigType) {
          // TODO: do I need to do anything in here?
        }

        override fun reloaded(configType: ConfigType, reloadedConfigs: List<Config>) {
//          if (configType == ConfigType.JES_WORKING_SET_CONFIG_V1) {
//            reloadedConfigs.forEach { config ->
//              config as JesWorkingSetConfig
//              explorerTreeStructure.registerProfileNode(
//                JesProfileNode(
//                  project,
//                  JesProfileNodeData(
//                    config.uuid,
//                    config.name
//                  ),
//                  explorerTreeStructure.rootElement
//                )
//              )
//            }
//          }
//           TODO: check if the reloaded config needs to be updated in the component
        }

        override fun added(config: Config) {
//          if (config is JesWorkingSetConfig) {
//            explorerTreeStructure.registerProfileNode(
//              JesProfileNode(
//                project,
//                JesProfileNodeDescriptor(config.name, config),
//                explorerTreeStructure.rootElement
//              )
//            )
//          }
//           TODO: any added profiles need to be added to the component
        }

        override fun updated(oldConfig: Config, newConfig: Config) {
          // TODO: most probably on update we need to update the component (username or IP change, other things, refresh)
          // TODO: when a related profile is updated, it needs to be refreshed in the view
        }

        override fun deleted(config: Config) {
          // TODO: when a config, related to the component is deleted, the component should be updated as well with the respective message
        }
      }
    )
  }
}