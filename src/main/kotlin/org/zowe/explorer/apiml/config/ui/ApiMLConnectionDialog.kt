/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.apiml.config.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.removeTrailingSlashes
import org.zowe.explorer.utils.validateConnectionName
import org.zowe.explorer.utils.validateForBlank
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

class ApiMLConnectionDialog(
  private val crudable: Crudable,
  override var state: ApiMLConnectionDialogState = ApiMLConnectionDialogState(),
  val project: Project? = null
): StatefulDialog<ApiMLConnectionDialogState>(project) {

  init {
    init()
    title = when(state.mode) {
      DialogMode.CREATE -> "Add API ML Connection"
      DialogMode.UPDATE, DialogMode.READ, DialogMode.DELETE -> "Edit API ML Connection"
    }
  }

  private lateinit var urlField: JTextField

  private lateinit var passwordField: JPasswordField

  private lateinit var acceptSelfSignedCheckbox: JCheckBox

  override fun createCenterPanel(): JComponent {
    val sameWidthLabelsGroup = "CONNECTION_DIALOG_LABELS_WIDTH_GROUP"

    return panel {
      row {
        label("Connection name")
          .widthGroup(sameWidthLabelsGroup)
        if (state.zoweConfigPath == null) {
          textField()
            .bindText(state::connectionName)
            .validationOnApply {
              it.text = it.text.trim()
              validateForBlank(it) ?: validateConnectionName(
                it,
                state.connectionName.ifBlank { null },
                crudable
              )
            }
            .focused()
            .align(AlignX.FILL)
        } else {
          textField()
            .bindText(state::connectionName)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      }
      row {
        label("Base Connection URL: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::connectionUrl)
          .validationOnApply {
            it.text = it.text.trim().removeTrailingSlashes()
            validateForBlank(it) /*?: validateZosmfUrl(it)*/
          }
          .also {
            urlField = it.component
            it.component.emptyText.setText("http(s)://host:port")
          }
          .align(AlignX.FILL)
      }
      row {
        label("ZOSMF Base Path")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::basePath)
          .validationOnApply { null }
          .align(AlignX.FILL)
      }
      row {
        label("API ML Gateway Path")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::gatewayPath)
          .validationOnApply { null }
          .align(AlignX.FILL)
      }
      row {
        label("Username: ")
          .widthGroup(sameWidthLabelsGroup)
        (
            if (state.zoweConfigPath == null)
              textField()
            else
              cell(JPasswordField())
            )
          .bindText(state::username)
          .validationOnApply {
            if (it !is JPasswordField)
              it.text = it.text.trim()
            validateForBlank(it)
          }.onApply {
            state.username = state.username.uppercase()
          }
          .align(AlignX.FILL)
      }
      row {
        label("Password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::password)
          .validationOnApply {
            validateForBlank(it) }
          .align(AlignX.FILL)
          .also {
            passwordField = it.component
          }
      }
      indent {
//        row {
//          checkBox("Save credentials")
//            .bindSelected(state::saveCredentials)
//        }
        row {
          checkBox("Accept self-signed SSL certificates")
            .bindSelected(state::isAllowSelfSigned)
            .also {
              it.component.apply {
                addActionListener {
                  if (this.isSelected) {
                    // showSelfSignedUsageWarningDialog(this)
                  }
                }
                acceptSelfSignedCheckbox = this
              }
            }
          icon(AllIcons.General.ContextHelp)
            .also {
              val sslHelpText =
                """Select this checkbox if your organization uses self-signed certificates (not recommended)."""
                  .trimMargin()
              HelpTooltip().setDescription(sslHelpText).installOn(it.component)
            }
        }
      }
    }
  }

}