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

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.api.api
import org.zowe.explorer.apiml.config.ApiMLConnectionConfig
import org.zowe.explorer.apiml.config.ui.table.ApiMLConnectionTableModel
import org.zowe.explorer.common.ui.*
import org.zowe.explorer.config.ConfigSandbox
import org.zowe.explorer.config.SandboxListener
import org.zowe.explorer.config.connect.*
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.dataops.operations.ZOSInfoOperation
import org.zowe.explorer.utils.isThe
import org.zowe.explorer.utils.runTask
import org.zowe.explorer.utils.subscribe
import org.zowe.kotlinsdk.ApiMLGetawayApi
import org.zowe.kotlinsdk.LoginRequest
import org.zowe.kotlinsdk.annotations.ZVersion

class ApiMLConnectionConfigurable : BoundSearchableConfigurable("API ML Connections", "mainframe") {

  private lateinit var tableModel: ApiMLConnectionTableModel
  private lateinit var table: ValidatingTableView<ApiMLConnectionDialogState>
  private lateinit var panel: DialogPanel

  override fun createPanel(): DialogPanel {
    tableModel = ApiMLConnectionTableModel(ConfigSandbox.getService().crudable)
    table = ValidatingTableView(tableModel, disposable!!)
      .apply {
        rowHeight = DEFAULT_ROW_HEIGHT
        // addMouseListener(registerMouseListener())
      }

    addSandboxListener()

    return panel {
      group("API ML Connections", false) {
        row {
          tableWithToolbar(table) {
            configureDecorator {
              setAddAction {
                addSession()
              }
              setEditAction {
                editSession()
              }
            }
          }
        }
          .resizableRow()
      }
        .resizableRow()
    }.also {
      panel = it
    }
  }

  private fun addSandboxListener() {
    subscribe(
      topic = SandboxListener.TOPIC,
      handler = object : SandboxListener {
        override fun <E : Any> update(clazz: Class<out E>) {
        }

        override fun <E : Any> reload(clazz: Class<out E>) {
          if (clazz.isThe<ConnectionConfig>()) {
            tableModel.reinitialize()
          }
        }

      },
      disposable = disposable!!
    )
  }

  override fun apply() {
    val wasModified = isModified
    ConfigSandbox.getService().apply(Credentials::class.java)
    ConfigSandbox.getService().apply(ApiMLConnectionConfig::class.java)
    if (wasModified) {
      panel.updateUI()
    }
  }

  override fun reset() {
    val wasModified = isModified
    ConfigSandbox.getService().rollback(Credentials::class.java)
    ConfigSandbox.getService().rollback(ApiMLConnectionConfig::class.java)
    if (wasModified) {
      panel.updateUI()
    }
  }

  override fun cancel() {
    reset()
  }

  override fun isModified(): Boolean {
    return ConfigSandbox.getService().isModified(Credentials::class.java)
        || ConfigSandbox.getService().isModified(ApiMLConnectionConfig::class.java)
  }

  private fun addSession() {
    val state = ApiMLConnectionDialogState().initEmptyUuids(ConfigSandbox.getService().crudable)
    showAndTestConnection(state, null)?.let { tableModel.addRow(it) }
  }

  private fun editSession() {
    table.selectedObject?.clone()?.let { state ->
      state.mode = DialogMode.UPDATE
      val dialog = ApiMLConnectionDialog(ConfigSandbox.getService().crudable, state)
      if (dialog.showAndGet()) {
        val idx = table.selectedRow
        tableModel[idx] = state
      }
    }
  }

  private fun showAndTestConnection(initialState: ApiMLConnectionDialogState, project: Project? = null): ApiMLConnectionDialogState? {
    val dialog = ApiMLConnectionDialog(ConfigSandbox.getService().crudable, initialState)
    return showUntilDone(
      initialState = initialState,
      factory = { dialog },
      test = { state ->
        val apiMLGatewayApiUrl = "${state.connectionUrl}$DEFAULT_GATEWAY_PATH/"
        val response = api<ApiMLGetawayApi>(url = apiMLGatewayApiUrl, true)
          .login(body = LoginRequest(state.username, state.password))
          .execute()
        val cookies = response.headers().values("Set-Cookie")
        val tokenVariableName = "apimlAuthenticationToken"
        val apimlAuthenticationToken = cookies
          .first { it.contains(tokenVariableName) }
          .substringAfter("$tokenVariableName=")
          .substringBefore(";")
        state.initEmptyUuids(ConfigSandbox.getService().crudable)
        val connectionConfig = state.connectionConfig
        CredentialService.getService().setCredentials(
          connectionConfigUuid = state.connectionUuid,
          username = state.username,
          password = state.password,
          token = apimlAuthenticationToken
        )
        val throwable = runTask(title = "Testing Connection to ${connectionConfig.url}", project = project) {
          return@runTask try {
            runCatching {
              DataOpsManager.getService().performOperation(InfoOperation(connectionConfig), it)
            }.onSuccess {
              val systemInfo = DataOpsManager.getService().performOperation(ZOSInfoOperation(connectionConfig))
              state.zVersion = when (systemInfo.zosVersion) {
                "04.25.00" -> ZVersion.ZOS_2_2
                "04.26.00" -> ZVersion.ZOS_2_3
                "04.27.00" -> ZVersion.ZOS_2_4
                "04.28.00" -> ZVersion.ZOS_2_5
                else -> ZVersion.ZOS_2_1
              }
              connectionConfig.zVersion = state.zVersion
            }.onFailure {
              throw it
            }
            null
          } catch (t: Throwable) {
            t
          }
        }
        if (throwable == null) {
          runTask(title = "Retrieving user information", project = project) {
            // Could be empty if TSO request fails
            state.owner = whoAmI(connectionConfig) ?: ""
          }
        }
        true
      }
    )
  }

}