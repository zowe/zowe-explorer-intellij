/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.settings.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.utils.validateBatchSize
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Class that represents Settings tab in preferences */
class SettingsConfigurable : BoundSearchableConfigurable("Settings", "mainframe") {
  private val configService = service<ConfigService>()
  private var panel: DialogPanel? = null
  private var isAutoSyncEnabled = AtomicBoolean(configService.isAutoSyncEnabled)
  private var isAutoSyncEnabledInitial = AtomicBoolean(isAutoSyncEnabled.get())

  private var batchSize = AtomicInteger(configService.batchSize)
  private var batchSizeInitial = AtomicInteger(batchSize.get())

  /** Settings panel description */
  override fun createPanel(): DialogPanel {
    return panel {
      group("Other Settings") {
        row {
          label("Batch amount to show per fetch")
          intTextField(IntRange(0, Int.MAX_VALUE))
            .bindIntText({ batchSize.get() }, { batchSize.set(it) })
            .validationOnInput { validateBatchSize(it) }
            .also { cell -> cell.component.whenTextChanged { batchSize.set(cell.component.text.toIntOrNull() ?: 100) } }
        }
        row {
          checkBox("Enable auto-sync with mainframe")
            .bindSelected({ isAutoSyncEnabled.get() }, { isAutoSyncEnabled.set(it) })
            .also { res ->
              res.component.addItemListener { isAutoSyncEnabled.set(res.component.isSelected) }
            }
        }
      }
    }
      .also { panel = it }
  }

  /** Reset previously set values to the initial ones */
  override fun reset() {
    configService.isAutoSyncEnabled = isAutoSyncEnabledInitial.get()
    isAutoSyncEnabled.set(isAutoSyncEnabledInitial.get())

    configService.batchSize = batchSizeInitial.get()
    batchSize.set(batchSizeInitial.get())
  }

  /** Apply all the changes */
  override fun apply() {
    configService.isAutoSyncEnabled = isAutoSyncEnabled.get()
    isAutoSyncEnabledInitial.set(isAutoSyncEnabled.get())

    configService.batchSize = batchSize.get()
    batchSizeInitial.set(batchSize.get())
  }

  /** Check is the changes were made */
  override fun isModified(): Boolean {
    return configService.isAutoSyncEnabled != isAutoSyncEnabled.get() || configService.batchSize != batchSize.get()
  }

  /** Cancel all the changes */
  override fun cancel() {
    isAutoSyncEnabled.set(configService.isAutoSyncEnabled)
    batchSize.set(configService.batchSize)
  }
}
