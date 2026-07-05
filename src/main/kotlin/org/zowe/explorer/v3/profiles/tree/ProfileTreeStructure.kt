/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.profiles.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.profiles.ProfileType
import org.zowe.explorer.v3.tree.ExplorerTreeStructure
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.ProfileNodeDescriptor

abstract class ProfileTreeStructure(private val project: Project) : ExplorerTreeStructure(project) {
  protected abstract val profileType: ProfileType

  protected abstract fun buildProfileDescriptor(configType: ConfigType, profileName: String): ProfileNodeDescriptor

  override fun addEntriesFromConfig() {
    val zoweConfigService = ZoweConfigService.getService()
    val configType = zoweConfigService.getSelectedConfigType(project)
    zoweConfigService.readProfileNames(profileType, configType, project.basePath)
      .forEach { profileName ->
        registerNode(
          ExplorerTreeNode(
            buildProfileDescriptor(configType, profileName),
            project,
            rootNode
          )
        )
      }
  }

  // TODO: doc
  override fun syncEntriesWithConfig() {
    val zoweConfigService = ZoweConfigService.getService()
    val configType = zoweConfigService.getSelectedConfigType(project)
    val configProfileNames = zoweConfigService.readProfileNames(profileType, configType, project.basePath)

    val existingByName = rootNode.treeNodes.associateBy { it.nodeDescriptor.displayName }

    val toRemove = existingByName.keys - configProfileNames.toSet()
    toRemove.forEach { name ->
      existingByName[name]?.let { unregisterNode(it) }
    }

    val toAdd = configProfileNames.toSet() - existingByName.keys
    toAdd.forEach { name ->
      registerNode(
        ExplorerTreeNode(
          buildProfileDescriptor(configType, name),
          project,
          rootNode
        )
      )
    }

    val toUpdate = configProfileNames.filter { it in existingByName && it !in toRemove }
    toUpdate.forEach { name ->
      existingByName[name]?.let { node ->
        node.nodeDescriptor = buildProfileDescriptor(configType, name)
      }
    }

    val updatedByName = rootNode.treeNodes.associateBy { it.nodeDescriptor.displayName }
    val sorted = configProfileNames.mapNotNull { updatedByName[it] }
    rootNode.treeNodes.clear()
    rootNode.treeNodes.addAll(sorted)
  }
}
