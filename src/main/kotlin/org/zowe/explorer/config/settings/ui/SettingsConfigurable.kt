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

package org.zowe.explorer.config.settings.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.rateus.RateUsNotification
import org.zowe.explorer.utils.validateBatchSize
import org.zowe.explorer.utils.validateJobReturnCode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Class that represents Settings tab in preferences */
class SettingsConfigurable : BoundSearchableConfigurable("Settings", "mainframe") {
  private val configService = ConfigService.getService()
  private var panel: DialogPanel? = null
  private var isAutoSyncEnabled = AtomicBoolean(configService.isAutoSyncEnabled)
  private var isAutoSyncEnabledInitial = AtomicBoolean(isAutoSyncEnabled.get())

  private var batchSize = AtomicInteger(configService.batchSize)
  private var successMaxCode = AtomicInteger(configService.successMaxCode)
  private var warningMaxCode = AtomicInteger(configService.warningMaxCode)
  private var batchSizeInitial = AtomicInteger(batchSize.get())
  private var successMaxCodeInitial = AtomicInteger(successMaxCode.get())
  private var warningMaxCodeInitial = AtomicInteger(warningMaxCode.get())
  private var isReturnCodesValid = true

  private lateinit var successField: JBTextField
  private lateinit var warningField: JBTextField

  /** Settings panel description */
  override fun createPanel(): DialogPanel {

    class ValidateJobReturnCode(
      var components: List<JBTextField>
    ) : DialogValidation {
      override fun validate(): ValidationInfo? {
        panel?.validateAll()
        var validationInfo: ValidationInfo?
        components.forEach { component ->
          validationInfo =
            validateJobReturnCode(successField, successMaxCodeInitial, warningField, warningMaxCodeInitial, component)
          if (validationInfo != null) {
            isReturnCodesValid = false
            return validationInfo
          }
        }
        isReturnCodesValid = true
        return null
      }
    }

    return panel {
      group("JES Explorer") {
        row {
          label("Max RC to consider as success")
          intTextField(IntRange(0, Int.MAX_VALUE))
            .bindIntText({ successMaxCode.get() }, { successMaxCode.set(it) })
            .also { successField = it.component }
            .also { cell ->
              cell.component.whenTextChanged {
                successMaxCode.set(
                  cell.component.text.toIntOrNull() ?: 0
                )
              }
            }
        }
        row {
          label("Max RC to consider as warning")
          intTextField(IntRange(0, Int.MAX_VALUE))
            .bindIntText({ warningMaxCode.get() }, { warningMaxCode.set(it) })
            .also { warningField = it.component }
            .also { cell ->
              cell.component.whenTextChanged {
                warningMaxCode.set(
                  cell.component.text.toIntOrNull() ?: 7
                )
              }
            }
        }
        row {
          label("Other RC value will be considered as an error")
        }
      }

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
        row {
          button("Clear File Cache") {
            var cacheCleared = false
            runModalTask("Cache Clearing", cancellable = false) {
              cacheCleared = DataOpsManager.getService().clearFileCache()
            }
            if (cacheCleared) {
              Messages.showInfoMessage(
                "The file cache has been successfully cleared.",
                "Cache Cleared",
              )
            }
          }.applyToComponent {
            toolTipText =
              "Clear the local contents of files downloaded from the remote system. All related files opened in the editor will be closed"
          }
        }
      }
      group("Rate Us") {
        row {
          label("If you want to leave a review:")
          @Suppress("DialogTitleCapitalization")
          link("click here") {
            configService.rateUsNotificationDelay = -1
            BrowserUtil.browse(RateUsNotification.PLUGIN_REVIEW_LINK)
          }
        }
      }
    }
      .apply {
        validationsOnInput = mapOf(
          successField to listOf(ValidateJobReturnCode(listOf(successField, warningField))),
          warningField to listOf(ValidateJobReturnCode(listOf(warningField, successField)))
        )
      }
      .also { panel = it }
  }

  /** Reset previously set values to the initial ones */
  override fun reset() {
    configService.isAutoSyncEnabled = isAutoSyncEnabledInitial.get()
    isAutoSyncEnabled.set(isAutoSyncEnabledInitial.get())

    configService.batchSize = batchSizeInitial.get()
    batchSize.set(batchSizeInitial.get())

    configService.successMaxCode = successMaxCodeInitial.get()
    successMaxCode.set(successMaxCodeInitial.get())

    configService.warningMaxCode = warningMaxCodeInitial.get()
    warningMaxCode.set(warningMaxCodeInitial.get())

    super.reset()
    panel?.updateUI()
  }

  /** Apply all the changes */
  override fun apply() {
    configService.isAutoSyncEnabled = isAutoSyncEnabled.get()
    isAutoSyncEnabledInitial.set(isAutoSyncEnabled.get())

    configService.batchSize = batchSize.get()
    batchSizeInitial.set(batchSize.get())

    if (isReturnCodesValid) {
      configService.successMaxCode = successMaxCode.get()
      successMaxCodeInitial.set(successMaxCode.get())

      configService.warningMaxCode = warningMaxCode.get()
      warningMaxCodeInitial.set(warningMaxCode.get())
    }
  }

  /** Check is the changes were made */
  override fun isModified(): Boolean {
    return (configService.isAutoSyncEnabled != isAutoSyncEnabled.get()
      || configService.batchSize != batchSize.get()
      || configService.successMaxCode != successMaxCode.get()
      || configService.warningMaxCode != warningMaxCode.get())
      && isReturnCodesValid
  }

  /** Cancel all the changes */
  override fun cancel() {
    isAutoSyncEnabled.set(configService.isAutoSyncEnabled)
    batchSize.set(configService.batchSize)
    successMaxCode.set(configService.successMaxCode)
    warningMaxCode.set(configService.warningMaxCode)
  }
}
