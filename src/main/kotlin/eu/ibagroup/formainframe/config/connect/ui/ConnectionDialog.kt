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

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.InfoOperation
import eu.ibagroup.formainframe.dataops.operations.ZOSInfoOperation
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.runTask
import eu.ibagroup.formainframe.utils.validateConnectionName
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateZosmfUrl
import eu.ibagroup.r2z.annotations.ZVersion
import java.awt.Component
import java.util.*
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

/** Dialog to add a new connection */
class ConnectionDialog(
  private val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState(),
  project: Project? = null
) : StatefulDialog<ConnectionDialogState>(project) {

  companion object {

    /** Show Test connection dialog and test the connection regarding the dialog state.
     * First the method checks whether connection succeeds for specified user/password.
     * If connection succeeds then the method automatically fill in z/OS version for this connection.
     * We do not need to worry about choosing z/OS version manually from combo box.
     * */
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
              runCatching {
                service<DataOpsManager>().performOperation(InfoOperation(state.connectionConfig), it)
              }.onSuccess {
                val systemInfo = service<DataOpsManager>().performOperation(ZOSInfoOperation(state.connectionConfig))
                state.zVersion = when (systemInfo.zosVersion) {
                  "04.25.00" -> ZVersion.ZOS_2_2
                  "04.26.00" -> ZVersion.ZOS_2_3
                  "04.27.00" -> ZVersion.ZOS_2_4
                  "04.28.00" -> ZVersion.ZOS_2_5
                  else -> ZVersion.ZOS_2_1
                }
              }.onFailure {
                throw it
              }
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

  /** Create dialog with the fields */
  override fun createCenterPanel(): JComponent {
    val sameWidthLabelsGroup = "CONNECTION_DIALOG_LABELS_WIDTH_GROUP"

    return panel {
      row {
        label("Connection name: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::connectionName)
          .validationOnApply {
            it.text = it.text.trim()
            validateForBlank(it) ?: validateConnectionName(
              it,
              initialState.connectionName.ifBlank { null },
              crudable
            )
          }
          .focused()
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Connection URL: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::connectionUrl)
          .validationOnApply {
            it.text = it.text.trim()
            validateForBlank(it) ?: validateZosmfUrl(it)
          }
          .also { urlTextField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Username: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::username)
          .validationOnApply {
            it.text = it.text.trim()
            validateForBlank(it)
          }.onApply {
            state.username = state.username.uppercase()
          }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Password: ")
          .widthGroup(sameWidthLabelsGroup)
        cell(JPasswordField())
          .bindText(state::password)
          .validationOnApply { validateForBlank(it) }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      indent {
        row {
          checkBox("Accept self-signed SSL certificates")
            .bindSelected(state::isAllowSsl)
            .also { sslCheckbox = it.component }
        }
      }
      if (state.mode == DialogMode.UPDATE) {
        row {
          label("z/OS Version")
            .widthGroup(sameWidthLabelsGroup)
          comboBox(
            listOf(
              ZVersion.ZOS_2_1,
              ZVersion.ZOS_2_2,
              ZVersion.ZOS_2_3,
              ZVersion.ZOS_2_4,
              ZVersion.ZOS_2_5
            )
          )
            .bindItem(state::zVersion.toNullableProperty())
            .enabled(false)
        }
      }
    }
      .withMinimumWidth(500)
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
