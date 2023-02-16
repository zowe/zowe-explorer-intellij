/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.utils.validateForBlank
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JPasswordField

/** Dialog to change user password */
class ChangePasswordDialog(
  override var state: ChangePasswordDialogState = ChangePasswordDialogState(),
  project: Project? = null
) : StatefulDialog<ChangePasswordDialogState>(project) {

  init {
    init()
    title = "Change user password"
  }

  lateinit var passField: JPasswordField

  /** Create dialog with the fields */
  override fun createCenterPanel(): JComponent {
    val sameWidthLabelsGroup = "CHANGE_PASSWORD_DIALOG_LABELS_WIDTH_GROUP"

    return panel {
      row {
        label("Username: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::username)
          .validationOnApply {
            it.text = it.text.trim()
            validateForBlank(it)
          }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Old password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::oldPassword)
          .validationOnApply { validateForBlank(it) }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("New password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::newPassword)
          .also { passField = it.component }
          .validationOnApply { validateForBlank(it) }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      indent {
        row {
          checkBox("Show password")
            .applyToComponent {
              addItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                  passField.echoChar = 0.toChar()
                } else {
                  passField.echoChar = '*'
                }
              }
            }
        }
      }
    }
  }


}
