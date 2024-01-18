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
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.TabbedConfigurable
import com.intellij.openapi.progress.runBackgroundableTask
import org.zowe.explorer.config.settings.ui.SettingsConfigurable

/**
 * Main UI class to build configurables for project and set them to appropriate place
 */
class MainframeConfigurable : TabbedConfigurable() {

  /**
   * Method which is called to display project name in Intellij
   */
  override fun getDisplayName(): String {
    return "Zowe Explorer"
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
      .mapNotNull { it.getConfigurable() as Configurable? }
      .distinct()
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
    runBackgroundableTask(title = "Reset changes", cancellable = false) {
      ConfigSandbox.instance.fetch()
      super.reset()
    }
  }

  /**
   * @see com.intellij.openapi.options.UnnamedConfigurable.cancel
   */
  override fun cancel() {
    configurables.forEach { it.cancel() }
  }

}
