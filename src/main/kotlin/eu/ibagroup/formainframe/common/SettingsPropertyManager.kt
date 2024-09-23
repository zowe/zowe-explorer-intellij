/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.common

import java.util.*

class SettingsPropertyManager

/**
 * Properties from the settings.properties file
 */
internal val settingsProperties by lazy {
  Properties().apply {
    load(SettingsPropertyManager::class.java.classLoader.getResourceAsStream("settings.properties"))
  }
}

/**
 * Check if the debug mode is enabled
 */
fun isDebugModeEnabled(): Boolean {
  return settingsProperties.getProperty("debug.mode")?.toBoolean() ?: false
}
