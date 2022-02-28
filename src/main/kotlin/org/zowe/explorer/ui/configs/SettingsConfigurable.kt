/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.ui.configs

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.panel
import org.zowe.explorer.config.ConfigService
import java.util.concurrent.atomic.AtomicBoolean

class SettingsConfigurable : BoundSearchableConfigurable("Settings", "mainframe") {
  private val configService = service<ConfigService>()
  private var panel: DialogPanel? = null
  private var isAutoSyncEnabled: AtomicBoolean = AtomicBoolean(configService.isAutoSyncEnabled.get())
  private var isAutoSyncEnabledInitial: AtomicBoolean = AtomicBoolean(isAutoSyncEnabled.get())
  private var isAutoSyncEnabledComponent: JBCheckBox? = null

  override fun createPanel(): DialogPanel {
    return panel {
//      Not applicable in this release
//      titledRow("Other Settings") {
//        row {
//          checkBox("Enable auto-sync with mainframe", isAutoSyncEnabled.get())
//            .also { res ->
//              isAutoSyncEnabledComponent = res.component
//              res.component.addItemListener { isAutoSyncEnabled.set(res.component.isSelected) }
//            }
//        }
//      }
    }.also { panel = it }
  }

  override fun reset() {
    configService.isAutoSyncEnabled.set(isAutoSyncEnabledInitial.get())
    isAutoSyncEnabled.set(isAutoSyncEnabledInitial.get())
    isAutoSyncEnabledComponent?.isSelected = isAutoSyncEnabled.get()
  }

  override fun apply() {
    configService.isAutoSyncEnabled.set(isAutoSyncEnabled.get())
    isAutoSyncEnabledInitial.set(isAutoSyncEnabled.get())
  }

  override fun isModified(): Boolean {
    return configService.isAutoSyncEnabled.get() != isAutoSyncEnabled.get()
  }

  override fun cancel() {
    isAutoSyncEnabled.set(configService.isAutoSyncEnabled.get())
  }
}
