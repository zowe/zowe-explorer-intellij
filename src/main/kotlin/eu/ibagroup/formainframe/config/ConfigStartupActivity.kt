/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager.PostStartupActivity

class ConfigStartupActivity: PostStartupActivity() {
  override fun runActivity(project: Project) {
    service<ConfigService>().apply {
      registerAllConfigClasses()
      service<OldConfigService>().state?.let { oldState ->
        migrateOldConfigState(oldState)
      }
    }
  }
}