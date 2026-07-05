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
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.profiles.ProfileType
import org.zowe.explorer.v3.profiles.tree.ProfileTreeStructure
import org.zowe.explorer.v3.tree.nodes.ProfileNodeDescriptor
import org.zowe.explorer.v3.tree.nodes.RootNode

// TODO: doc
class TsoSessionsTreeStructure(private val project: Project) : ProfileTreeStructure(project) {
  override val rootNode by lazy { RootNode(project) }
  override val profileType = ProfileType.TSO_IJ

  // TODO: implement
  override fun buildProfileDescriptor(configType: ConfigType, profileName: String): ProfileNodeDescriptor {
    return ProfileNodeDescriptor("Not implemented", "Not yet implemented")
  }
}
