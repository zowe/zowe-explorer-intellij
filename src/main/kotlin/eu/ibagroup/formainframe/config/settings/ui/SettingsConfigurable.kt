/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.settings.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.PolicyProvider
import eu.ibagroup.formainframe.analytics.ui.AnalyticsPolicyDialog
import eu.ibagroup.formainframe.config.ConfigService
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JLabel

/** Class that represents Settings tab in preferences */
class SettingsConfigurable : BoundSearchableConfigurable("Settings", "mainframe") {
  private val analyticsService = service<AnalyticsService>()
  private val policyProvider = service<PolicyProvider>()
  private val configService = service<ConfigService>()
  private val agreed = "you have agreed to the collection and processing of data"
  private val notAgreed = "you haven't agreed to the collection and processing of data"
  private var agreementLabelComponent: JLabel? = null
  private var panel: DialogPanel? = null
  private var isAutoSyncEnabled: AtomicBoolean = AtomicBoolean(configService.isAutoSyncEnabled.get())
  private var isAutoSyncEnabledInitial: AtomicBoolean = AtomicBoolean(isAutoSyncEnabled.get())
  private var isAutoSyncEnabledComponent: JBCheckBox? = null

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
    configService.isAutoSyncEnabled.set(isAutoSyncEnabledInitial.get())
    isAutoSyncEnabled.set(isAutoSyncEnabledInitial.get())
    isAutoSyncEnabledComponent?.isSelected = isAutoSyncEnabled.get()
  }

  /** Apply all the changes */
  override fun apply() {
    configService.isAutoSyncEnabled.set(isAutoSyncEnabled.get())
    isAutoSyncEnabledInitial.set(isAutoSyncEnabled.get())
  }

  /** Check is the changes were made */
  override fun isModified(): Boolean {
    return configService.isAutoSyncEnabled.get() != isAutoSyncEnabled.get()
  }

  /** Cancel all the changes */
  override fun cancel() {
    isAutoSyncEnabled.set(configService.isAutoSyncEnabled.get())
  }
}
