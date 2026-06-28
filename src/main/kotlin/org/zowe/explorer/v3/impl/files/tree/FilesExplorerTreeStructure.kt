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
import org.zowe.explorer.v3.profiles.ProfileType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.ExplorerTreeStructure
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.RootNode

/**
 * Tree structure for the Files Explorer.
 * Populates the tree with files profile nodes read from the selected Zowe Team Config
 */
class FilesExplorerTreeStructure(private val project: Project) : ExplorerTreeStructure(project) {
  override val rootNode by lazy { RootNode(project) }

  /**
   * Reads `files_ij` profiles from the `explorer_ij` section of the active Zowe config
   * and registers them as top-level profile nodes in the tree
   */
  fun addFilesProfilesFromConfig() {
    val configType = ZoweConfigService.getService().getSelectedConfigType(project)
    ZoweConfigService.getService().readProfileNames(ProfileType.FILES_IJ, configType, project.basePath)
      .forEach { profileName ->
        registerProfileNode(
          ExplorerTreeNode(
            buildFilesProfileDescriptor(configType, profileName),
            project,
            rootNode
          )
        )
      }
  }

  /**
   * Diffs current profile nodes against the active Zowe config and applies
   * only the necessary additions/removals, preserving existing nodes' state
   * (e.g. expanded/collapsed). Maintains the same order as in the config file
   */
  fun syncProfilesWithConfig() {
    val configService = ZoweConfigService.getService()
    val configType = configService.getSelectedConfigType(project)
    val configProfileNames = configService.readProfileNames(ProfileType.FILES_IJ, configType, project.basePath)

    val existingByName = rootNode.profileNodes.associateBy { it.nodeDescriptor.displayName }

    val toRemove = existingByName.keys - configProfileNames.toSet()
    toRemove.forEach { name ->
      existingByName[name]?.let { unregisterProfileNode(it) }
    }

    val toAdd = configProfileNames.toSet() - existingByName.keys
    toAdd.forEach { name ->
      registerProfileNode(
        ExplorerTreeNode(
          buildFilesProfileDescriptor(configType, name),
          project,
          rootNode
        )
      )
    }

    val toUpdate = configProfileNames.filter { it in existingByName && it !in toRemove }
    toUpdate.forEach { name ->
      existingByName[name]?.let { node ->
        node.nodeDescriptor = buildFilesProfileDescriptor(configType, name)
      }
    }

    val updatedByName = rootNode.profileNodes.associateBy { it.nodeDescriptor.displayName }
    val sorted = configProfileNames.mapNotNull { updatedByName[it] }
    rootNode.profileNodes.clear()
    rootNode.profileNodes.addAll(sorted)
  }

  private fun buildFilesProfileDescriptor(
    configType: org.zowe.explorer.v3.impl.teamconfig.ConfigType,
    profileName: String
  ): FilesProfileNodeDescriptor {
    val configService = ZoweConfigService.getService()
    val connectionProfilePath = configService.readConnectionProfile(configType, project.basePath, profileName)
    val dsMasks = configService.readDsMasks(configType, project.basePath, profileName)
    val ussFilters = configService.readUssFilters(configType, project.basePath, profileName)
    return FilesProfileNodeDescriptor(profileName, null, connectionProfilePath, dsMasks, ussFilters)
  }
}
