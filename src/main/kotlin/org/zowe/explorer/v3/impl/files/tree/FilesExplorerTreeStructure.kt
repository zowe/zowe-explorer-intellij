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
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.profiles.ProfileType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.profiles.tree.ProfileTreeStructure
import org.zowe.explorer.v3.tree.nodes.RootNode

/**
 * Tree structure for the Files Explorer.
 * Populates the tree with files profile nodes read from the selected Zowe Team Config
 */
class FilesExplorerTreeStructure(private val project: Project) : ProfileTreeStructure(project) {
  override val rootNode by lazy { RootNode(project) }
  override val profileType = ProfileType.FILES_IJ

  override fun buildProfileDescriptor(configType: ConfigType, profileName: String): FilesProfileNodeDescriptor {
    val zoweConfigService = ZoweConfigService.getService()
    val connectionProfilePath = zoweConfigService.readConnectionProfile(configType, project.basePath, profileName)
    val connectionProfileType = connectionProfilePath?.let {
      zoweConfigService.readProfileType(configType, project.basePath, it)
    }
    val dsMasks = zoweConfigService.readDsMasks(configType, project.basePath, profileName)
    val ussFilters = zoweConfigService.readUssFilters(configType, project.basePath, profileName)
    return FilesProfileNodeDescriptor(profileName, connectionProfilePath, connectionProfileType, dsMasks, ussFilters)
  }
}
