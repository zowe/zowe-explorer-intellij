package eu.ibagroup.formainframe.analytics

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import eu.ibagroup.formainframe.ui.analytics.AnalyticsPolicyDialog

class AnalyticsStartupActivity : StartupActivity {
  override fun runActivity(project: Project) {
    val analyticsService = service<AnalyticsService>()
    val policyProvider = service<PolicyProvider>()
    if (!analyticsService.isUserAcknowledged) {
      AnalyticsPolicyDialog.open(analyticsService, policyProvider, project)
    }
  }
}
