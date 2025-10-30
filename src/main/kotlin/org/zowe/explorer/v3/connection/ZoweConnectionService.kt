/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.connection

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.zowe.kotlinsdk.core.ZoweProfileManager
import org.zowe.kotlinsdk.core.connectivity.ZoweConnectionManager

// TODO: doc
@Service
class ZoweConnectionService {
  companion object {
    fun getService() = service<ZoweConnectionService>()
  }

  private val zoweConnectivityByProject = mutableMapOf<Project, Pair<ZoweProfileManager, ZoweConnectionManager>>()

  fun initConnectivity(project: Project) {
    zoweConnectivityByProject.getOrPut(project) {
      val zoweProfileManager = ZoweProfileManager()
        .apply { teamConfigDir = project.basePath }
      val zoweConnectionManager = ZoweConnectionManager(zoweProfileManager)
      zoweProfileManager to zoweConnectionManager
    }
  }

  fun getZoweConnectionManager(project: Project): ZoweConnectionManager {
    return zoweConnectivityByProject[project]?.second
      ?: throw Exception("Zowe connection manager is not initialized for the project '$project'")
  }
}
