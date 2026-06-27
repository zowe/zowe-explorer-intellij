/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.vault.tree

import com.intellij.openapi.project.Project
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.impl.vault.tree.nodes.SecureProfileNodeDescriptor
import org.zowe.explorer.v3.tree.ExplorerTreeStructure
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.RootNode

/**
 * Tree structure for the Secure Vault explorer.
 * Populates the tree with all profile nodes from the active Zowe Team Config.
 * Profiles with non-empty `secure` arrays are expandable; others are leaf nodes
 */
class SecureVaultTreeStructure(private val project: Project) : ExplorerTreeStructure(project) {
  override val rootNode by lazy { RootNode(project) }

  /**
   * Reads all profiles from the active Zowe config
   * and registers them as top-level nodes in the tree
   */
  fun addProfilesFromConfig() {
    val configService = ZoweConfigService.getService()
    val configType = configService.getSelectedConfigType(project)
    configService.readAllProfiles(configType, project.basePath)
      .forEach { entry ->
        registerProfileNode(
          ExplorerTreeNode(
            SecureProfileNodeDescriptor(entry.profilePath, entry.secureFields),
            project,
            rootNode
          )
        )
      }
  }

  /**
   * Diffs current profile nodes against the active Zowe config and applies
   * only the necessary additions/removals, preserving existing nodes' state.
   * Replaces nodes whose `secure` fields have changed.
   * Maintains the same order as in the config file
   */
  fun syncProfilesWithConfig() {
    val configService = ZoweConfigService.getService()
    val configType = configService.getSelectedConfigType(project)
    val configEntries = configService.readAllProfiles(configType, project.basePath)
    val configPaths = configEntries.map { it.profilePath }
    val entriesByPath = configEntries.associateBy { it.profilePath }

    val existingByPath = rootNode.profileNodes.associateBy { it.nodeDescriptor.displayName }

    val toRemove = existingByPath.keys - configPaths.toSet()
    toRemove.forEach { path ->
      existingByPath[path]?.let { unregisterProfileNode(it) }
    }

    val toUpdate = existingByPath.keys.intersect(configPaths.toSet()).filter { path ->
      val existing = existingByPath[path]?.nodeDescriptor as? SecureProfileNodeDescriptor
      val updated = entriesByPath[path]
      existing != null && updated != null && existing.secureFields != updated.secureFields
    }
    toUpdate.forEach { path ->
      val descriptor = existingByPath[path]?.nodeDescriptor as? SecureProfileNodeDescriptor ?: return@forEach
      val entry = entriesByPath[path] ?: return@forEach
      descriptor.updateSecureFields(entry.secureFields)
    }

    val toAdd = configPaths.toSet() - existingByPath.keys
    toAdd.forEach { path ->
      val entry = entriesByPath[path] ?: return@forEach
      registerProfileNode(
        ExplorerTreeNode(
          SecureProfileNodeDescriptor(entry.profilePath, entry.secureFields),
          project,
          rootNode
        )
      )
    }

    val updatedByPath = rootNode.profileNodes.associateBy { it.nodeDescriptor.displayName }
    val sorted = configPaths.mapNotNull { updatedByPath[it] }
    rootNode.profileNodes.clear()
    rootNode.profileNodes.addAll(sorted)
  }
}
