/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.analytics

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import eu.ibagroup.formainframe.analytics.ui.AnalyticsPolicyDialog

/**
 * Class for registering startup action for achieving user agreement with analytics policy.
 * It shows the analytics policy dialog before starting events tracking.
 * @author Valiantsin Krus.
 */
class AnalyticsStartupActivity : ProjectActivity {

  /** Shows analytics policy dialog if user was not aware of it. */
  override suspend fun execute(project: Project) {
    val analyticsService = service<AnalyticsService>()
    val policyProvider = service<PolicyProvider>()
    if (!analyticsService.isUserAcknowledged) {
      runInEdt {
        AnalyticsPolicyDialog.open(analyticsService, policyProvider, project)
      }
    }
  }
}
