/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect.ui.zosmf

import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.*
import eu.ibagroup.formainframe.config.connect.ui.ChangePasswordDialog
import eu.ibagroup.formainframe.config.connect.ui.ChangePasswordDialogState
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.*
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.utils.crudable.getAll
import org.zowe.kotlinsdk.ChangePassword
import org.zowe.kotlinsdk.annotations.ZVersion
import java.awt.Component
import java.awt.Point
import java.util.*
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

/** Dialog to add a new connection */
class ConnectionDialog(
  private val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState(),
  val project: Project? = null
) : StatefulDialog<ConnectionDialogState>(project) {

  /**
   * Private field
   * In case of DialogMode.UPDATE takes the last successful state from crudable, takes default state otherwise
   */
  private val lastSuccessfulState: ConnectionDialogState =
    if(state.mode == DialogMode.UPDATE) crudable.find<ConnectionConfig> { it.uuid == state.connectionUuid }
      .findAny()
      .orElseGet { state.connectionConfig }
      .toDialogState(crudable) else ConnectionDialogState()
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
          val newTestedConnConfig : ConnectionConfig
          if (initialState.mode == DialogMode.UPDATE) {
            val newState = state.clone()
            newState.initEmptyUuids(crudable)
            newTestedConnConfig = ConnectionConfig(newState.connectionUuid, newState.connectionName, newState.connectionUrl, newState.isAllowSsl, newState.zVersion)
            CredentialService.instance.setCredentials(
              connectionConfigUuid = newState.connectionUuid,
              username = newState.username,
              password = newState.password
            )
          } else {
            state.initEmptyUuids(crudable)
            newTestedConnConfig = state.connectionConfig
            CredentialService.instance.setCredentials(
              connectionConfigUuid = state.connectionUuid,
              username = state.username,
              password = state.password)
          }
          val throwable = runTask(title = "Testing Connection to ${newTestedConnConfig.url}", project = project) {
            return@runTask try {
              runCatching {
                service<DataOpsManager>().performOperation(InfoOperation(newTestedConnConfig), it)
              }.onSuccess {
                val systemInfo = service<DataOpsManager>().performOperation(ZOSInfoOperation(newTestedConnConfig))
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
            runTask(title = "Retrieving user information", project = project) {
              // Could be empty if TSO request fails
              state.owner = whoAmI(newTestedConnConfig) ?: ""
            }
            if (state.owner.isEmpty()) showWarningNotification(project)
            true
          }
        }
      )
    }

    /**
     * Function shows a warning notification if USS owner cannot be retrieved
     */
    private fun showWarningNotification(project: Project?) {
      Notification(
        EXPLORER_NOTIFICATION_GROUP_ID,
        "Unable to retrieve USS username",
        "Cannot retrieve USS username. An error happened while executing TSO request.\n" +
            "When working with USS files the same username will be used that was specified by the user when connecting.",
        NotificationType.WARNING
      ).let {
        Notifications.Bus.notify(it, project)
      }
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
    lateinit var passField: Cell<JPasswordField>

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
            it.text = it.text.trim().removeTrailingSlashes()
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
        passField = cell(JPasswordField())
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
