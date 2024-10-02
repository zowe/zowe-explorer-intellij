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

package eu.ibagroup.formainframe.config.connect.ui.zosmf

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.ui.ChangePasswordDialog
import eu.ibagroup.formainframe.config.connect.ui.ChangePasswordDialogState
import eu.ibagroup.formainframe.config.connect.whoAmI
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CredentialsNotFoundForConnectionException
import eu.ibagroup.formainframe.dataops.operations.ChangePasswordOperation
import eu.ibagroup.formainframe.dataops.operations.InfoOperation
import eu.ibagroup.formainframe.dataops.operations.ZOSInfoOperation
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
    if (state.mode == DialogMode.UPDATE) crudable.find<ConnectionConfig> { it.uuid == state.connectionUuid }
      .findAny()
      .orElseGet { state.connectionConfig }
      .toDialogState(crudable) else ConnectionDialogState()

  companion object {

    /**
     * Check if only the connection name has changed in the connection dialog
     */
    private fun isOnlyConnectionNameChanged(
      initialState: ConnectionDialogState,
      state: ConnectionDialogState
    ): Boolean {
      return initialState.connectionName != state.connectionName &&
        initialState.connectionUrl == state.connectionUrl &&
        initialState.username == state.username &&
        initialState.password == state.password &&
        initialState.isAllowSsl == state.isAllowSsl
    }

    /**
     * Companion function which takes the instance of ConnectionDialog and checks its current state after OK button is pressed.
     * @param dialog
     * @return returns true if there are violations found, if no violations were found then returns false as a result of validation
     */
    private fun validateSecureConnectionUsage(dialog: ConnectionDialog): Boolean {
      val urlToCheck = dialog.urlTextField.text.trim().startsWith("http://", true)
      val sslEnabledCheck = dialog.sslCheckbox.isSelected
      if (urlToCheck || sslEnabledCheck) {
        return dialog.showSelfSignedUsageWarningDialog(dialog.urlTextField, dialog.sslCheckbox)
      }
      return false
    }

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
      var connectionDialog = ConnectionDialog(crudable, initialState, project)
      val initState = initialState.clone()
      return showUntilDone(
        initialState = initialState,
        factory = { connectionDialog },
        test = { state ->

          if (validateSecureConnectionUsage(connectionDialog)) {
            state.connectionUrl = connectionDialog.urlTextField.text
            state.isAllowSsl = connectionDialog.sslCheckbox.isSelected
            connectionDialog = ConnectionDialog(crudable, state, project)
            return@showUntilDone false
          }

          val newTestedConnConfig: ConnectionConfig
          if (initialState.mode == DialogMode.UPDATE) {
            if (isOnlyConnectionNameChanged(initState, state)) {
              return@showUntilDone true
            }
            val newState = state.clone()
            newState.initEmptyUuids(crudable)
            newTestedConnConfig = ConnectionConfig(
              newState.connectionUuid,
              newState.connectionName,
              newState.connectionUrl,
              newState.isAllowSsl,
              newState.zVersion
            )
            CredentialService.getService().setCredentials(
              connectionConfigUuid = newState.connectionUuid,
              username = newState.username,
              password = newState.password
            )
          } else {
            state.initEmptyUuids(crudable)
            newTestedConnConfig = state.connectionConfig
            CredentialService.getService().setCredentials(
              connectionConfigUuid = state.connectionUuid,
              username = state.username,
              password = state.password
            )
          }
          val throwable = runTask(title = "Testing Connection to ${newTestedConnConfig.url}", project = project) {
            return@runTask try {
              runCatching {
                DataOpsManager.getService().performOperation(InfoOperation(newTestedConnConfig), it)
              }.onSuccess {
                val systemInfo = DataOpsManager.getService().performOperation(ZOSInfoOperation(newTestedConnConfig))
                state.zVersion = when (systemInfo.zosVersion) {
                  "04.25.00" -> ZVersion.ZOS_2_2
                  "04.26.00" -> ZVersion.ZOS_2_3
                  "04.27.00" -> ZVersion.ZOS_2_4
                  "04.28.00" -> ZVersion.ZOS_2_5
                  else -> ZVersion.ZOS_2_1
                }
                newTestedConnConfig.zVersion = state.zVersion
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
            val tMessage = if (throwable is ProcessCanceledException) {
              message("explorer.cancel.by.user.error")
            } else {
              throwable.message?.let {
                if (it.contains("Exception")) {
                  it.substring(it.lastIndexOf(":") + 2)
                    .replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
                } else {
                  it
                }
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
            connectionDialog = ConnectionDialog(crudable, state, project)
            // In case of any error during connection, we should clean the owner field, because actually it cannot be retrieved at this moment
            // Clear owner field only in case the user pressed "add anyway", leave old owner as it is otherwise
            addAnyway.also { runIfTrue(it) { state.owner = "" } }
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
          .align(AlignX.FILL)
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
          .component.emptyText.setText("http(s)://host:port")
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
          icon(AllIcons.General.ContextHelp)
            .also {
              val sslHelpText =
                """Select this checkbox if your organization uses self-signed certificates (not recommended)."""
                  .trimMargin()
              HelpTooltip().setDescription(sslHelpText).installOn(it.component)
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
              val dataOpsManager = DataOpsManager.getService()
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

                      ConfigService.getService().crudable
                        .getAll<ConnectionConfig>()
                        .filter { it.url == state.connectionUrl }
                        .filter { CredentialService.getService().getUsernameByKey(it.uuid) == state.username }
                        .forEach {
                          CredentialService.getService().setCredentials(
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
      var username = lastSuccessfulState.username
      var password = lastSuccessfulState.password
      try {
        username = CredentialService.getUsername(lastSuccessfulState.connectionConfig)
        password = CredentialService.getPassword(lastSuccessfulState.connectionConfig)
      } catch (_: CredentialsNotFoundForConnectionException) {
      }
      state.connectionName = lastSuccessfulState.connectionName
      state.connectionUrl = lastSuccessfulState.connectionUrl
      state.username = username
      state.password = password
      state.isAllowSsl = lastSuccessfulState.isAllowSsl
      state.zVersion = lastSuccessfulState.zVersion
      runBackgroundableTask("Setting credentials", project, false) {
        CredentialService.getService().setCredentials(
          connectionConfigUuid = lastSuccessfulState.connectionUuid,
          username = username,
          password = password
        )
      }
    }
  }

  /**
   * Function shows the warning dialog if any violations found and resolves them in case "Back to safety" was clicked
   * @param components - dialog components which have to be resolved to valid values
   * @return result of the pressed button
   */
  private fun showSelfSignedUsageWarningDialog(vararg components: Component): Boolean {
    // default return backToSafety
    val backToSafety = true
    val choice = Messages.showDialog(
      project,
      "Creating an unsecure connection (HTTP instead of HTTP(s) and/or using self-signed certificates) is not recommended.\n" +
        "You do this at your own peril and risk, and we do not bear any responsibility for the possible consequences of using this type of connection.\n" +
        "Please contact your system administrator to configure your system to be able to create a secure connection.\n\n" +
        "Do you want to proceed anyway?",
      "Attempt to create an unsecured connection",
      arrayOf(
        "Back to safety",
        "Proceed"
      ),
      0,
      AllIcons.General.WarningDialog,
      null
    )
    return when (choice) {
      0 -> {
        components.forEach {
          if (it is JBCheckBox) it.isSelected = false
          if (it is JBTextField) it.text = it.text.replace("http://", "https://", true)
        }
        backToSafety
      }

      1 -> !backToSafety
      else -> backToSafety
    }
  }

}
