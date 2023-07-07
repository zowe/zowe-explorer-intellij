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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent

/**
 * Config service to read old configs.
 * @author Valiantsin Krus
 */
interface OldConfigService : PersistentStateComponent<ConfigState> {
  companion object {
    @JvmStatic
    val instance: ConfigService
      get() = ApplicationManager.getApplication().getService(ConfigService::class.java)
  }
}
