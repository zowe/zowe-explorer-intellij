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
import org.zowe.explorer.v3.impl.jes.tree.nodes.JesProfileNodeDescriptor
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.profiles.ProfileType
import org.zowe.explorer.v3.profiles.tree.ProfileTreeStructure
import org.zowe.explorer.v3.tree.nodes.RootNode

/**
 * Tree structure for the JES Explorer.
 * Populates the tree with JES profile nodes read from the selected Zowe Team Config
 */
class JesExplorerTreeStructure(private val project: Project) : ProfileTreeStructure(project) {
  override val rootNode by lazy { RootNode(project) }
  override val profileType = ProfileType.JES_IJ

  override fun buildProfileDescriptor(configType: ConfigType, profileName: String): JesProfileNodeDescriptor {
    val zoweConfigService = ZoweConfigService.getService()
    val connectionProfilePath = zoweConfigService.readConnectionProfile(configType, project.basePath, profileName)
    val connectionProfileType = connectionProfilePath?.let {
      zoweConfigService.readProfileType(configType, project.basePath, it)
    }
    val jobFilters = zoweConfigService.readJobFilters(configType, project.basePath, profileName)
    return JesProfileNodeDescriptor(profileName, connectionProfilePath, connectionProfileType, jobFilters)
  }
}