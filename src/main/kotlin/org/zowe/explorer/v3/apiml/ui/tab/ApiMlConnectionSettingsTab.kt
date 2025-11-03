/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.apiml.ui.tab

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.common.ui.*
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.ZOSInfoOperation
import org.zowe.explorer.utils.runTask
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.v3.Requester
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigEventListener
import org.zowe.explorer.v3.apiml.ui.ApiMlConnectionDialog
import org.zowe.explorer.v3.apiml.ui.ApiMlConnectionDialogState
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.ApiMlConnectionConfig
import org.zowe.explorer.v3.state.credentials.cache.CredentialsCacheService
import org.zowe.explorer.v3.apiml.ui.isOnlyConnectionNameChanged
import org.zowe.explorer.v3.apiml.ui.table.ApiMlConnectionTableModel
import org.zowe.explorer.v3.operations.OperationsService
import org.zowe.explorer.v3.operations.apiml.ApiMlLoginOperationData
import org.zowe.explorer.v3.operations.system.GetSystemsOperationData
import org.zowe.explorer.v3.operations.system.SystemInfoOperationData
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig

class ApiMlConnectionSettingsTab : BoundSearchableConfigurable("API ML Connections", "mainframe") {

  private lateinit var tableModel: ApiMlConnectionTableModel
  private lateinit var table: ValidatingTableView<ApiMlConnectionDialogState>
  private lateinit var panel: DialogPanel

  override fun createPanel(): DialogPanel {
    tableModel = ApiMlConnectionTableModel()
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
      topic = ConfigCacheService.TOPIC,
      handler = object: ConfigEventListener {
        override fun registered(configType: ConfigType) {
        }

        override fun added(config: Config) {
        }

        override fun updated(oldConfig: Config, newConfig: Config) {
        }

        override fun deleted(config: Config) {
        }

        override fun reloaded(configType: ConfigType, reloadedConfigs: List<Config>) {
          if (configType == ConfigType.API_ML_CONNECTION_CONFIG_V1) {
            tableModel.reinitialize()
          }
        }
      },
      disposable = disposable!!
    )
  }

  override fun apply() {
    val wasModified = isModified
    ConfigCacheService.getService().saveCacheToStorage(ConfigType.API_ML_CONNECTION_CONFIG_V1)
    CredentialsCacheService.getService().saveCacheToStorage()
    if (wasModified) {
      panel.updateUI()
    }
  }

  override fun reset() {
    val wasModified = isModified
    ConfigCacheService.getService().reloadConfigsFromStorage()
    CredentialsCacheService.getService().reloadCredentialsFromStorage(ConfigType.API_ML_CONNECTION_CONFIG_V1)
    if (wasModified) {
      panel.updateUI()
    }
  }

  override fun cancel() {
    reset()
  }

  override fun isModified(): Boolean {
    return ConfigCacheService.getService().isCacheModified(ConfigType.API_ML_CONNECTION_CONFIG_V1)
      || CredentialsCacheService.getService().isCacheModified()
  }

  override fun disposeUIResources() {
    CredentialsCacheService.getService().clearCache()
    super.disposeUIResources()
  }

  private fun addSession() {
    val state = ApiMlConnectionDialogState()
    showAndTestConnection(state, null)?.let { tableModel.addRow(it) }
  }

  private fun editSession() {
    table.selectedObject?.let { state ->
      state.mode = DialogMode.UPDATE
      val dialog = ApiMlConnectionDialog(state)
      if (dialog.showAndGet()) {
        val idx = table.selectedRow
        tableModel[idx] = state
      }
    }
  }

  private fun showAndTestConnection(initialState: ApiMlConnectionDialogState, project: Project? = null): ApiMlConnectionDialogState? {
    val dialog = ApiMlConnectionDialog(initialState, project)
    return showUntilDone(
      initialState = initialState,
      factory = { dialog },
      test = { state ->

        //validateSecureConnectionUsage

        val apiMlConnectionConfig: ApiMlConnectionConfig
        when(initialState.mode) {
          DialogMode.UPDATE -> {
            if (isOnlyConnectionNameChanged(initialState, state)) {
              return@showUntilDone true
            }
            apiMlConnectionConfig = state.apiMlConnectionConfig
          }
          else -> {
            apiMlConnectionConfig = state.apiMlConnectionConfig
          }
        }
        val throwable =  runTask(title = "Testing Connection to ${apiMlConnectionConfig.host}", project = project) {
          runCatching {
            OperationsService.getService()
              .performOperation(
                operationData = ApiMlLoginOperationData(
                  username = state.username,
                  password = state.password,
                  origin = object: Requester<ApiMlConnectionConfig> {
                    override val connectionConfig = apiMlConnectionConfig
                  }
                ),
                progressIndicator = it
              )

            OperationsService.getService()
              .performOperation(
                operationData = GetSystemsOperationData(
                  origin = object: Requester<HttpConnectionConfig> {
                    override val connectionConfig = apiMlConnectionConfig
                  }
                ),
                progressIndicator = it
              )

            val systemInfo = OperationsService.getService()
              .performOperation(
                operationData = SystemInfoOperationData(
                  origin = object: Requester<HttpConnectionConfig> {
                    override val connectionConfig = apiMlConnectionConfig
                  }
                ),
                progressIndicator = it
              ).getOrThrow()

            //DataOpsManager.getService().performOperation(InfoOperation(apiMlConnectionConfig), it)
            //val systemInfo = DataOpsManager.getService().performOperation(ZOSInfoOperation(apiMlConnectionConfig))

            state.zVersion = systemInfo.zVersion
            apiMlConnectionConfig.zVersion = state.zVersion

          }.exceptionOrNull()
        }
        true
      }
    )
  }

}