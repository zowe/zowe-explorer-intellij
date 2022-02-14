/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.analytics

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.zowe.explorer.ui.analytics.AnalyticsPolicyDialog

class AnalyticsStartupActivity : StartupActivity {
  override fun runActivity(project: Project) {
    val analyticsService = service<AnalyticsService>()
    val policyProvider = service<PolicyProvider>()
    if (!analyticsService.isUserAcknowledged) {
      AnalyticsPolicyDialog.open(analyticsService, policyProvider, project)
    }
  }
}
