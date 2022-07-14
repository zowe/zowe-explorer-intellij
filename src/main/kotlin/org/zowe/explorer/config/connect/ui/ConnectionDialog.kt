/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.connect.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.toBinding
import com.intellij.ui.layout.withTextBinding
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runTask
import org.zowe.explorer.utils.validateConnectionName
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.utils.validateZosmfUrl
import org.zowe.kotlinsdk.CodePage
import org.zowe.kotlinsdk.annotations.ZVersion
import java.awt.Component
import java.util.*
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

class ConnectionDialog(
  private val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState(),
  project: Project? = null
) : StatefulDialog<ConnectionDialogState>(project) {

  companion object {
    @JvmStatic
    fun showAndTestConnection(
      crudable: Crudable,
      parentComponent: Component? = null,
      project: Project? = null,
      initialState: ConnectionDialogState
    ): ConnectionDialogState? {
      return showUntilDone(
        initialState = initialState,
        factory = { ConnectionDialog(crudable, initialState, project) },
        test = { state ->
          val throwable = runTask(title = "Testing Connection to ${state.connectionConfig.url}", project = project) {
            return@runTask try {
              CredentialService.instance.setCredentials(
                connectionConfigUuid = state.connectionUuid,
                username = state.username,
                password = state.password
              )
              val info = service<DataOpsManager>().performOperation(InfoOperation(state.connectionConfig), it)
              state.zVersion = info.getZOSVersion()
              null
            } catch (t: Throwable) {
              t
            }
          }
          if (throwable != null) {
            state.mode = DialogMode.UPDATE
            val confirmMessage = "Do you want to add it anyway?"
            val tMessage = throwable.message?.let {
              if (it.contains("Exception")) {
                it.substring(it.lastIndexOf(":") + 2)
                  .replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
              } else {
                it
              }
            }
            val message = if (tMessage != null) {
              "$tMessage\n\n$confirmMessage"
            } else {
              confirmMessage
            }
            val addAnyway = MessageDialogBuilder
              .yesNo(
                title = "Error Creating Connection",
                message = message
              ).icon(AllIcons.General.ErrorDialog)
              .run {
                if (parentComponent != null) {
                  ask(parentComponent)
                } else {
                  ask(project)
                }
              }
            addAnyway
          } else {
            true
          }
        }
      )
    }
  }

  private val initialState = state.clone()

  private lateinit var urlTextField: JTextField

  private lateinit var sslCheckbox: JCheckBox

  init {
    isResizable = false
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Connection name")
        if (state.zoweConfigPath == null) {
          textField(state::connectionName)
            .focused()
            .withValidationOnInput {
              validateConnectionName(
                it,
                initialState.connectionName.ifBlank { null },
                crudable
              )
            }
            .withValidationOnApply {
              it.text = it.text.trim()
              validateForBlank(it)
            }
        } else {
          JBTextField(state.connectionName).apply { isEditable = false }()
        }
      }
      row {
        label("Connection URL")
        textField(state::connectionUrl).withValidationOnApply {
          it.text = it.text.trim()
          validateForBlank(it) ?: validateZosmfUrl(it)
        }.also { urlTextField = it.component }
      }
      row {
        label("Username")
        (if (state.zoweConfigPath == null) textField(state::username)
        else JPasswordField(state.username)().withTextBinding(state::username.toBinding()))
          .withValidationOnApply {
            it.text = it.text.trim()
            validateForBlank(it)
          }
      }
      row {
        label("Password")
        JPasswordField(state.password)().withTextBinding(state::password.toBinding()).withValidationOnApply {
          validateForBlank(it)
        }
      }
      row {
        checkBox("Accept self-signed SSL certificates", state::isAllowSsl)
          .withLargeLeftGap().also { sslCheckbox = it.component }
      }
      row {
        label("Code Page")
        comboBox(
          model = CollectionComboBoxModel(
            listOf(
              CodePage.IBM_1025,
              CodePage.IBM_1047

            )
          ),
          prop = state::codePage
        )
      }
      if (state.mode == DialogMode.UPDATE) {
        row {
          label("z/OS Version")
          comboBox(
            model = CollectionComboBoxModel(
              listOf(
                ZVersion.ZOS_2_1,
                ZVersion.ZOS_2_2,
                ZVersion.ZOS_2_3,
                ZVersion.ZOS_2_4,
                ZVersion.ZOS_2_5
              )
            ),
            prop = state::zVersion
          )
        }
      }
    }.withMinimumWidth(500)
  }

  init {
    init()
    title = when (state.mode) {
      DialogMode.READ -> "Connection Properties"
      DialogMode.DELETE -> "Delete Connection"
      DialogMode.UPDATE -> "Edit Connection"
      DialogMode.CREATE -> "Add Connection"
    }
  }

}
