package eu.ibagroup.formainframe.analytics.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UIUtil
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.PolicyProvider
import eu.ibagroup.formainframe.analytics.ui.AnalyticsPolicyDialog.Companion.buildLicenceComponent

class AnalyticsConfigurable : BoundSearchableConfigurable("Analytics", "mainframe") {

  private val analyticsService = service<AnalyticsService>()
  private val policyProvider = service<PolicyProvider>()
  private var analyticsEnabled: Boolean = analyticsService.isAnalyticsEnabled

  override fun createPanel(): DialogPanel {
    return panel {
      buildLicenceComponent(
        policyText = policyProvider.text,
        agreementText = policyProvider.buildAgreementText("selecting"),
        numberOfColumns = 70,
        fontSize = UIUtil.FontSize.SMALL
      )
      row {
        checkBox("I Agree", analyticsEnabled).apply {
          component.addItemListener {
            analyticsEnabled = this.component.isSelected
          }
        }
      }
    }
  }


  override fun apply() {
    analyticsService.isAnalyticsEnabled = analyticsEnabled
  }

  override fun isModified(): Boolean {
    return analyticsService.isAnalyticsEnabled != analyticsEnabled
  }

  override fun cancel() {
    analyticsEnabled = analyticsService.isAnalyticsEnabled
  }
}