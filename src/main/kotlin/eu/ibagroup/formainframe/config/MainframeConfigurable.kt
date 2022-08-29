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

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.TabbedConfigurable
import eu.ibagroup.formainframe.config.connect.ui.ConnectionConfigurable
import eu.ibagroup.formainframe.config.ws.ui.files.WSConfigurable
import eu.ibagroup.formainframe.config.ws.ui.jobs.JobsWsConfigurable
import eu.ibagroup.formainframe.ui.configs.SettingsConfigurable

class MainframeConfigurable : TabbedConfigurable() {
  var preferredConfigurableClass: Class<*>? = null

  override fun getDisplayName(): String {
    return "For Mainframe"
  }

  private lateinit var connectionConfigurable: ConnectionConfigurable
  private lateinit var wsConfigurable: WSConfigurable
  private lateinit var jobsWsConfigurable: JobsWsConfigurable
  private lateinit var settingsConfigurable: SettingsConfigurable

  override fun createConfigurables(): MutableList<Configurable> {
    return mutableListOf(
      ConnectionConfigurable().also { connectionConfigurable = it },
      WSConfigurable().also { wsConfigurable = it },
      JobsWsConfigurable().also { jobsWsConfigurable = it },
      SettingsConfigurable().also { settingsConfigurable = it }
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
