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

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.TabbedConfigurable
import org.zowe.explorer.ui.configs.SettingsConfigurable
import org.zowe.explorer.config.connect.ui.ConnectionConfigurable
import org.zowe.explorer.config.ws.ui.jobs.JobsWsConfigurable
import org.zowe.explorer.config.ws.ui.files.WSConfigurable

class MainframeConfigurable : TabbedConfigurable() {
  var preferredConfigurableClass: Class<*>? = null

  override fun getDisplayName(): String {
    return "Zowe Explorer"
  }

  private lateinit var connectionConfigurable: ConnectionConfigurable
  private lateinit var wsConfigurable: WSConfigurable
  private lateinit var jobsWsConfigurable: JobsWsConfigurable
  private lateinit var settingsConfigurable: SettingsConfigurable

  override fun createConfigurables(): MutableList<Configurable> {
    return mutableListOf(
      WSConfigurable().also { wsConfigurable = it },
      ConnectionConfigurable().also { connectionConfigurable = it },
      JobsWsConfigurable().also { jobsWsConfigurable = it }
//      AnalyticsConfigurable().also { analyticsConfigurable = it }
    )
  }

  override fun apply() {
    super.apply()
    ConfigSandbox.instance.updateState()
  }

  override fun reset() {
    ConfigSandbox.instance.fetch()
    super.reset()
  }

  override fun cancel() {
    configurables.forEach { it.cancel() }
  }

  override fun createConfigurableTabs() {
    super.createConfigurableTabs().also {
      myTabbedPane.selectedIndex = when (preferredConfigurableClass) {
        SettingsConfigurable::class.java -> 4
        JobsWsConfigurable::class.java -> 3
        WSConfigurable::class.java -> 2
        ConnectionConfigurable::class.java -> 1
        else -> 0
      }
    }
  }
}
