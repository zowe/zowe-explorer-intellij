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

package eu.ibagroup.formainframe.analytics.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.plus
import com.intellij.util.ui.UIUtil
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.PolicyProvider
import java.awt.Dimension
import javax.swing.JComponent

class AnalyticsPolicyDialog(
  private val policyProvider: PolicyProvider,
  project: Project?
) : DialogWrapper(project, true) {

  companion object {

    /**
     * Open analytics policy dialog and wait until user selects any option
     * @param analyticsService the analytics service to track the user's choice
     * @param policyProvider the policy provider to show the agreement
     * @param project the project for a dialog window
     */
    fun open(analyticsService: AnalyticsService, policyProvider: PolicyProvider, project: Project?): Boolean {
      val dialog = AnalyticsPolicyDialog(policyProvider = policyProvider, project = project)
      val res = dialog.showAndGet()
      analyticsService.isAnalyticsEnabled = res
      analyticsService.isUserAcknowledged = true
      return res
    }
  }

  init {
    init()
    title = "For Mainframe Plugin Privacy Policy and Terms and Conditions"
    setOKButtonText("I Agree")
    setCancelButtonText("Dismiss")
  }

  /** Build license component with provided parameters */
  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        val licenseTextArea = JBTextArea(20, 90)
          .apply {
            text = policyProvider.text
            lineWrap = true
            wrapStyleWord = true
            caretPosition = 0
          }
        scrollCell(licenseTextArea)
          .align(AlignX.FILL.plus(AlignY.FILL))
      }.resizableRow()
      row {
        text(policyProvider.buildAgreementText("clicking"))
          .apply {
            component.font = UIUtil.getFont(UIUtil.FontSize.NORMAL, component.font)
            component.preferredSize = Dimension(660, 20)
          }
          .align(AlignX.FILL.plus(AlignY.FILL))
      }
    }
  }
}
