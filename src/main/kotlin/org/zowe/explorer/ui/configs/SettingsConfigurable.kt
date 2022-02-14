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
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.analytics.AnalyticsService
import org.zowe.explorer.analytics.PolicyProvider
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.ui.analytics.AnalyticsPolicyDialog
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JLabel

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

  private fun agreedOrDisagreed(isAnalyticsEnabled: Boolean): String {
    return if (isAnalyticsEnabled) { agreed } else { notAgreed }
  }

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow("Analytics") {
        row {
          button("Show the Privacy Policy") {
            AnalyticsPolicyDialog.open(analyticsService, policyProvider, null)
            agreementLabelComponent?.text = agreedOrDisagreed(analyticsService.isAnalyticsEnabled)
          }
          label(agreedOrDisagreed(analyticsService.isAnalyticsEnabled), UIUtil.ComponentStyle.SMALL)
            .also { agreementLabelComponent = it.component }
        }
      }
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
