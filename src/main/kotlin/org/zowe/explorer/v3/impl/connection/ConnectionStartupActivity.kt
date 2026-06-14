/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.connection

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

// TODO: doc
class ConnectionStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    ZoweConnectionService.getService()
      .initConnectivity(project)
  }
}
