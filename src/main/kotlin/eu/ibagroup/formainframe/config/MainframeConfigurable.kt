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

  /**
   * Method which is called to display project name in Intellij
   */
  override fun getDisplayName(): String {
    return "For Mainframe"
  }


  /**
   * Method to create all existed configurables and initialize them
   * @return List of configurable items
   */
  override fun createConfigurables(): MutableList<Configurable> {
    val configService = service<ConfigService>()
    return configService
      .getRegisteredConfigDeclarations()
      .sortedBy { it.configPriority }
      .mapNotNull { it.getConfigurable() }
      .toMutableList()
      .apply { add(SettingsConfigurable()) }
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

}
