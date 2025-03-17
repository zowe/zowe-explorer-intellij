/*
 * Copyright (c) 2024 IBA Group.
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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.state.storage

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.ConfigsHolder
import org.zowe.explorer.v3.state.settings.OtherSettingsHolder

/** Storage state that holds the plug-in specific settings */
class StorageState : ConfigsHolder, OtherSettingsHolder, BaseState() {
  override var isAutoSyncEnabled by property(true)
  override var batchSize by property(100)

  @get:Tag("configs")
  @get:XMap(entryTagName = "config", keyAttributeName = "type")
  override var configs by map<ConfigType, MutableList<Config>>()

  /**
   * Trigger modification counter increment.
   * Needed to notify IntelliJ that there are changes in our configs that should be saved
   */
  fun modificationPerformed() {
    incrementModificationCount()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is StorageState) return false
    if (!super.equals(other)) return false

    if (isAutoSyncEnabled != other.isAutoSyncEnabled) return false
    if (batchSize != other.batchSize) return false

    // Check that any config in the origin list differs from the list to compare
    val areConfigsDiffer = configs.entries
      .find { (configType, configsList) ->
        val otherConfigsList = other.configs[configType]
        otherConfigsList == null || !configsList.any { config -> otherConfigsList.contains(config) }
      } != null

    return !areConfigsDiffer
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + isAutoSyncEnabled.hashCode()
    result = 31 * result + batchSize.hashCode()
    result = 31 * result + configs.hashCode()
    return result
  }
}
