/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Activity to prepare configs.
 * @author Valiantsin Krus.
 */
class ConfigStartupActivity : ProjectActivity {

  /** Registers all config classes and migrate configs to state v2. */
  override suspend fun execute(project: Project) {
    service<ConfigService>().apply {
      registerAllConfigClasses()
    }
  }
}
