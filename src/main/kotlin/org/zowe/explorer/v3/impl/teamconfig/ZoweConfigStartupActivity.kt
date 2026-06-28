/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Registers the opened project in [ZoweConfigService.setSelectedConfigType]
 * with the default [ConfigType.LOCAL_TEAM] config type
 */
class ZoweConfigStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    ZoweConfigService.getService()
      .setSelectedConfigType(project, ConfigType.LOCAL_TEAM)
  }
}
