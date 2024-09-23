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

package eu.ibagroup.formainframe.config.settings.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.PolicyProvider
import eu.ibagroup.formainframe.analytics.ui.AnalyticsPolicyDialog
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.rateus.RateUsNotification
import eu.ibagroup.formainframe.utils.validateBatchSize
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JLabel

/** Class that represents Settings tab in preferences */
class SettingsConfigurable : BoundSearchableConfigurable("Settings", "mainframe") {
  private val analyticsService = AnalyticsService.getService()
  private val policyProvider = PolicyProvider.getService()
  private val configService = ConfigService.getService()
  private val agreed = "you have agreed to the collection and processing of data"
  private val notAgreed = "you haven't agreed to the collection and processing of data"
  private var agreementLabelComponent: JLabel? = null
  private var panel: DialogPanel? = null
  private var isAutoSyncEnabled = AtomicBoolean(configService.isAutoSyncEnabled)
  private var isAutoSyncEnabledInitial = AtomicBoolean(isAutoSyncEnabled.get())

  private var batchSize = AtomicInteger(configService.batchSize)
  private var batchSizeInitial = AtomicInteger(batchSize.get())

  /**
   * Check whether user is agreed or disagreed on analytics process
   * @param isAnalyticsEnabled value that represents is user agreed or disagreed on analytics process
   * @return label depending on user choice
   */
  private fun agreedOrDisagreed(isAnalyticsEnabled: Boolean): String {
    return if (isAnalyticsEnabled) {
      agreed
    } else {
      notAgreed
    }
  }

  /** Settings panel description */
  override fun createPanel(): DialogPanel {
    return panel {
      group("Analytics") {
        row {
          button("Show the Privacy Policy") {
            AnalyticsPolicyDialog.open(analyticsService, policyProvider, null)
            agreementLabelComponent?.text = agreedOrDisagreed(analyticsService.isAnalyticsEnabled)
          }
          label(agreedOrDisagreed(analyticsService.isAnalyticsEnabled))
            .also { agreementLabelComponent = it.component }
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
