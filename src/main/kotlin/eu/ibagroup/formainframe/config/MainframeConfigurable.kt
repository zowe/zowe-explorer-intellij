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
import eu.ibagroup.formainframe.config.settings.ui.SettingsConfigurable
import eu.ibagroup.formainframe.config.ws.ui.files.FilesWSConfigurable
import eu.ibagroup.formainframe.config.ws.ui.jes.JesWsConfigurable

/**
 * Main UI class to build configurables for project and set them to appropriate place
 */
class MainframeConfigurable : TabbedConfigurable() {
  var preferredConfigurableClass: Class<*>? = null

  /**
   * Method which is called to display project name in Intellij
   */
  override fun getDisplayName(): String {
    return "For Mainframe"
  }

  private lateinit var connectionConfigurable: ConnectionConfigurable
  private lateinit var wsConfigurable: FilesWSConfigurable
  private lateinit var jesWsConfigurable: JesWsConfigurable
  private lateinit var settingsConfigurable: SettingsConfigurable

  /**
   * Method to create all existed configurables and initialize them
   * @return List of configurable items
   */
  override fun createConfigurables(): MutableList<Configurable> {
    return mutableListOf(
      ConnectionConfigurable().also { connectionConfigurable = it },
      FilesWSConfigurable().also { wsConfigurable = it },
      JesWsConfigurable().also { jesWsConfigurable = it },
      SettingsConfigurable().also { settingsConfigurable = it }
    )
  }

  /**
   * @see com.intellij.openapi.options.UnnamedConfigurable.apply
   */
  override fun apply() {
    super.apply()
    ConfigSandbox.instance.updateState()
  }

  /**
   * @see com.intellij.openapi.options.UnnamedConfigurable.reset
   */
  override fun reset() {
    ConfigSandbox.instance.fetch()
    super.reset()
  }

  /**
   * @see com.intellij.openapi.options.UnnamedConfigurable.cancel
   */
  override fun cancel() {
    configurables.forEach { it.cancel() }
  }

  /**
   * Method to create configurable tabs and add(represent) them in UI
   */
  override fun createConfigurableTabs() {
    super.createConfigurableTabs().also {
      myTabbedPane.selectedIndex = when (preferredConfigurableClass) {
        SettingsConfigurable::class.java -> 4
        JesWsConfigurable::class.java -> 3
        FilesWSConfigurable::class.java -> 2
        ConnectionConfigurable::class.java -> 1
        else -> 0
      }
    }
  }
}
