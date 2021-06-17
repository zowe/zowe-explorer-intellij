/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.analytics.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UIUtil
import eu.ibagroup.formainframe.analytics.PolicyProvider
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

class AnalyticsPolicyDialog(
  private val policyProvider: PolicyProvider,
  project: Project
) : DialogWrapper(project, true) {

  companion object {
    fun LayoutBuilder.buildLicenceComponent(
      policyText: String,
      agreementText: String,
      numberOfColumns: Int = 90,
      fontSize: UIUtil.FontSize = UIUtil.FontSize.NORMAL
    ): Row {
      return with(this) {
        row {
          scrollPane(JBTextArea(20, numberOfColumns).apply {
            text = policyText
            lineWrap = true
            wrapStyleWord = true
            caretPosition = 0
          }).applyToComponent {
            verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
          }
        }
        row {
          label(agreementText).apply {
            component.font = UIUtil.getFont(fontSize, component.font)
          }
        }
      }
    }
  }

  init {
    init()
    title = "For Mainframe Plugin Privacy Policy and Terms and Conditions"
    setOKButtonText("I Agree")
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      buildLicenceComponent(policyProvider.text, policyProvider.buildAgreementText("clicking"))
    }
  }


}

