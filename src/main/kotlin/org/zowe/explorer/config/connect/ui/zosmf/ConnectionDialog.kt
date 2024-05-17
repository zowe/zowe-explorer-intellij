/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.connect.ui.zosmf

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.*
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.*
import org.zowe.explorer.config.connect.ui.ChangePasswordDialog
import org.zowe.explorer.config.connect.ui.ChangePasswordDialogState
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.*
import org.zowe.explorer.utils.*
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.find
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.kotlinsdk.ChangePassword
import org.zowe.kotlinsdk.annotations.ZVersion
import java.awt.Component
import java.awt.Point
import java.util.*
import javax.swing.*

/** Dialog to add a new connection */
class ConnectionDialog(
  crudable: Crudable,
  state: ConnectionDialogState = ConnectionDialogState(),
  project: Project? = null
) : CommonConnectionDialog(crudable, state, project) {

  /**
   * Private field
   * In case of DialogMode.UPDATE takes the last successful state from crudable, takes default state otherwise
   */
  private val lastSuccessfulState: ConnectionDialogState =
    if (state.mode == DialogMode.UPDATE) crudable.find<ConnectionConfig> { it.uuid == state.connectionUuid }
      .findAny()
      .orElseGet { state.connectionConfig }
      .toDialogState(crudable) else ConnectionDialogState()

  companion object {
    //Call showAndTestConnectionCommon for current class
    @JvmStatic
    fun showAndTestConnection(
      crudable: Crudable,
      parentComponent: Component? = null,
      project: Project? = null,
      initialState: ConnectionDialogState
    ): ConnectionDialogState? {
      val connectionDialog =
        ConnectionDialog(crudable, initialState, project)
      return connectionDialog.showAndTestConnectionCommon(crudable, parentComponent, project, initialState)
    }
  }

  private val initialState = state.clone()

  init {
    isResizable = false
  }

  override fun createConnectionDialog(
    crudable: Crudable,
    state: ConnectionDialogState,
    project: Project?
  ): CommonConnectionDialog {
    return ConnectionDialog(crudable, state, project)
  }

  /** Create dialog with the fields */
  override fun createCenterPanel(): JComponent {
    val sameWidthLabelsGroup = "CONNECTION_DIALOG_LABELS_WIDTH_GROUP"
    lateinit var passField: Cell<JPasswordField>

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
                initialState.connectionName.ifBlank { null },
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
        label("Connection URL: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::connectionUrl)
          .validationOnApply {
            it.text = it.text.trim().removeTrailingSlashes()
            validateForBlank(it) ?: validateZosmfUrl(it)
          }
          .also { urlTextField = it.component }
          .align(AlignX.FILL)
      }
      row {
        label("Username")
          .widthGroup(sameWidthLabelsGroup)
        (
            if (state.zoweConfigPath == null)
              textField()
            else
              cell(JPasswordField())
            )
          .bindText(state::username)
          .validationOnApply {
            validateForBlank(it.text.trim(), it)
          }
          .onApply {
            state.username = state.username.trim().uppercase()
          }
          .align(AlignX.FILL)
      }
      row {
        label("Password: ")
          .widthGroup(sameWidthLabelsGroup)
        passField = cell(JPasswordField())
          .bindText(state::password)
          .validationOnApply { validateForBlank(it) }
          .align(AlignX.FILL)
      }
      indent {
        row {
          checkBox("Accept self-signed SSL certificates")
            .bindSelected(state::isAllowSsl)
            .also {
              it.component.apply {
                addActionListener {
                  if (this.isSelected) {
                    showSelfSignedUsageWarningDialog(this)
                  }
                }
                sslCheckbox = this
              }
            }
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
        if (state.zVersion > ZVersion.ZOS_2_4) {
          row {
            button("Change user password") {
              val changePasswordInitState = ChangePasswordDialogState(state.username, state.password, "")
              val dataOpsManager = service<DataOpsManager>()
              showUntilDone(
                initialState = changePasswordInitState,
                factory = { ChangePasswordDialog(changePasswordInitState, project) },
                test = { changePasswordState ->
                  val throwable = runTask(
                    title = "Changing ${changePasswordState.username} password on ${initialState.connectionUrl}",
                    project = project
                  ) {
                    return@runTask try {
                      dataOpsManager.performOperation(
                        operation = ChangePasswordOperation(
                          request = ChangePassword(
                            changePasswordState.username,
                            changePasswordState.oldPassword,
                            changePasswordState.newPassword
                          ),
                          connectionConfig = state.connectionConfig
                        ),
                        progressIndicator = it
                      )
                      null
                    } catch (t: Throwable) {
                      t
                    }
                  }
                  if (throwable != null) {
                    val errorMessage = throwable.message!!.substring(0, throwable.message!!.indexOf('\n'))
                    val respMessage = throwable.message!!.substring(
                      throwable.message!!.indexOf("MESSAGE:"),
                      throwable.message!!.indexOf('\n', throwable.message!!.indexOf("MESSAGE:"))
                    )
                    val okCancelDialog = MessageDialogBuilder
                      .okCancel(
                        title = "Error",
                        message = "${errorMessage}\n\n${respMessage}"
                      ).icon(AllIcons.General.ErrorDialog)
                      .run {
                        ask(project)
                      }
                    okCancelDialog
                  } else {
                    if (state.username == changePasswordState.username) {
                      passField.applyToComponent {
                        this.text = changePasswordState.newPassword
                        state.password = changePasswordState.newPassword
                        val balloon = JBPopupFactory.getInstance()
                          .createHtmlTextBalloonBuilder(
                            "The password is substituted with the new value",
                            MessageType.INFO,
                            null
                          )
                          .setHideOnClickOutside(true)
                          .setHideOnLinkClick(true)
                          .createBalloon()
                        val relativePoint = RelativePoint(this, Point(this.width / 2, this.height))
                        balloon.show(relativePoint, Balloon.Position.below)
                      }

                      configCrudable.getAll<ConnectionConfig>().filter { it.url == state.connectionUrl }
                        .filter { CredentialService.instance.getUsernameByKey(it.uuid) == state.username }
                        .forEach {
                          CredentialService.instance.setCredentials(
                            connectionConfigUuid = it.uuid,
                            username = state.username,
                            password = state.password
                          )
                        }
                    }
                    true
                  }
                }
              )
            }
          }
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

  /**
   * Function to be performed when Cancel button is pressed.
   * Resets the values in connection dialog and connections table model to the last successful state
   * Updates the credentials to satisfy the last known successful state
   */
  override fun doCancelAction() {
    super.doCancelAction()
    if (state.mode == DialogMode.UPDATE) {
      state.connectionName = lastSuccessfulState.connectionName
      state.connectionUrl = lastSuccessfulState.connectionUrl
      state.username = getUsername(lastSuccessfulState.connectionConfig)
      state.password = getPassword(lastSuccessfulState.connectionConfig)
      state.isAllowSsl = lastSuccessfulState.isAllowSsl
      state.zVersion = lastSuccessfulState.zVersion
      runBackgroundableTask("Setting credentials", project, false) {
        CredentialService.instance.setCredentials(
          connectionConfigUuid = lastSuccessfulState.connectionUuid,
          username = getUsername(lastSuccessfulState.connectionConfig),
          password = getPassword(lastSuccessfulState.connectionConfig)
        )
      }
    }
  }


}
