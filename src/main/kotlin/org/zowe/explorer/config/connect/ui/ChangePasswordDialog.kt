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

package org.zowe.explorer.config.connect.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.utils.validateForPassword
import java.awt.BorderLayout
import java.awt.event.*
import javax.swing.*

/** Dialog to change user password */
class ChangePasswordDialog(
  override var state: ChangePasswordDialogState = ChangePasswordDialogState(),
  project: Project? = null
) : StatefulDialog<ChangePasswordDialogState>(project) {

  init {
    init()
    title = "Change user password"
  }

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
          .align(AlignX.FILL)
      }
      row {
        label("Old password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::oldPassword)
          .applyToComponent {
            this.layout = BorderLayout()
            addShowHidePasswordIcon(this)
          }
          .validationOnApply { validateForBlank(it) }
          .align(AlignX.FILL)
      }
      row {
        label("New password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::newPassword)
          .applyToComponent {
            this.layout = BorderLayout()
            addShowHidePasswordIcon(this)
            addFocusListener(object : FocusAdapter() {
              override fun focusLost(e: FocusEvent?) {
                state.newPassword = String(this@applyToComponent.password)
              }
            })
          }
          .validationOnApply { validateForBlank(it) }
          .align(AlignX.FILL)
      }
      row {
        label("Confirm password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::confirmPassword)
          .applyToComponent {
            this.layout = BorderLayout()
            addShowHidePasswordIcon(this)
          }
          .validationOnApply { validateForBlank(it) ?: validateForPassword(state.newPassword, it) }
          .align(AlignX.FILL)
      }
    }
  }

  /**
   * Add "eye" icon to show password
   * @param component [JPasswordField] instance to add icon
   */
  private fun addShowHidePasswordIcon(component: JPasswordField) {
    val defaultEchoChar = component.echoChar
    val eyeIcon = JLabel(AllIcons.General.InspectionsEye)

    eyeIcon.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        component.echoChar = '\u0000'
      }

      override fun mouseReleased(e: MouseEvent?) {
        component.echoChar = defaultEchoChar
      }
    })

    component.add(eyeIcon, BorderLayout.EAST)
  }


}
