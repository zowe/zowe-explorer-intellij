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

package org.zowe.explorer.config.connect.ui.zosmf

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import org.zowe.explorer.common.message
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.whoAmI
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.dataops.operations.ZOSInfoOperation
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runTask
import org.zowe.kotlinsdk.annotations.ZVersion
import java.awt.Component
import java.util.*
import javax.swing.JCheckBox
import javax.swing.JTextField

abstract class CommonConnectionDialog(
  protected val crudable: Crudable,
  override var state: ConnectionDialogState = ConnectionDialogState(),
  val project: Project? = null
) : StatefulDialog<ConnectionDialogState>(project) {


  protected lateinit var urlTextField: JTextField

  protected lateinit var sslCheckbox: JCheckBox

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
  private fun validateSecureConnectionUsage(dialog: CommonConnectionDialog): Boolean {
    val urlToCheck = dialog.urlTextField.text.trim().startsWith("http://", true)
    val sslEnabledCheck = dialog.sslCheckbox.isSelected
    if (urlToCheck || sslEnabledCheck) {
      return dialog.showSelfSignedUsageWarningDialog(dialog.urlTextField, dialog.sslCheckbox)
    }
    return false
  }

  /**
   * Function shows the warning dialog if any violations found and resolves them in case "Back to safety" was clicked
   * @param components - dialog components which have to be resolved to valid values
   * @return result of the pressed button
   */
  fun showSelfSignedUsageWarningDialog(vararg components: Component): Boolean {
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
          if (it is JBTextField) it.text = it.text.replace("http:", "https:", true)
        }
        backToSafety
      }

      1 -> !backToSafety
      else -> backToSafety
    }
  }

  abstract fun createConnectionDialog(
    crudable: Crudable,
    state: ConnectionDialogState = ConnectionDialogState(),
    project: Project? = null
  ): CommonConnectionDialog

  /** Show Test connection dialog and test the connection regarding the dialog state.
   * First the method checks whether connection succeeds for specified user/password.
   * If connection succeeds then the method automatically fill in z/OS version for this connection.
   * We do not need to worry about choosing z/OS version manually from combo box.
   * */
  fun showAndTestConnectionCommon(
    crudable: Crudable,
    parentComponent: Component? = null,
    project: Project? = null,
    initialState: ConnectionDialogState
  ): ConnectionDialogState? {
    var connectionDialog = this
    val initState = initialState.clone()
    return showUntilDone(
      initialState = initialState,
      factory = { connectionDialog },
      test = { state ->

        if (validateSecureConnectionUsage(connectionDialog)) {
          state.connectionUrl = connectionDialog.urlTextField.text
          state.isAllowSsl = connectionDialog.sslCheckbox.isSelected
          connectionDialog = createConnectionDialog(
            crudable,
            initialState,
            project
          )
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
              service<DataOpsManager>().performOperation(InfoOperation(newTestedConnConfig), it)
            }.onSuccess {
              state.owner = whoAmI(newTestedConnConfig) ?: ""
              val systemInfo =
                service<DataOpsManager>().performOperation(ZOSInfoOperation(newTestedConnConfig))
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
          connectionDialog = createConnectionDialog(
            crudable,
            initialState,
            project
          )
          addAnyway
        } else {
          true
        }
      }
    )
  }

}