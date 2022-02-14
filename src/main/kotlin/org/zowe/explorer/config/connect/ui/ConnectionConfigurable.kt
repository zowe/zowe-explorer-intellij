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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.layout.panel
import org.zowe.explorer.common.ui.DEFAULT_ROW_HEIGHT
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.ValidatingTableView
import org.zowe.explorer.common.ui.toolbarTable
import org.zowe.explorer.config.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.isThe
import org.zowe.explorer.utils.runWriteActionOnWriteThread
import org.zowe.explorer.utils.toMutableList
import eu.ibagroup.r2z.zowe.config.ZoweConfig
import eu.ibagroup.r2z.zowe.config.parseConfigJson
import java.net.URI

@Suppress("DialogTitleCapitalization")
class ConnectionConfigurable : BoundSearchableConfigurable("z/OSMF Connections", "mainframe") {

  var openAddDialog = false

  private fun showAndTestConnection(initialState: ConnectionDialogState = ConnectionDialogState()): ConnectionDialogState? {
    return ConnectionDialog.showAndTestConnection(
      crudable = sandboxCrudable,
      parentComponent = panel?.components?.getOrNull(0),
      initialState = initialState
    )
  }

  private fun addConnection() {
    showAndTestConnection()?.let { connectionsTableModel?.addRow(it) }
  }

  private fun ZoweConfig.updateFromState (state: ConnectionDialogState) {
    val uri = URI(state.connectionUrl)
    host = uri.host
    port = uri.port.toLong()
    protocol = state.connectionUrl.split("://")[0]
    user = state.username
    password = state.password
    codePage = state.codePage
    rejectUnauthorized = !state.isAllowSsl
  }

  /**
   * Shows confirm dialog and if user say yes than updates zowe.config.json related to specified conneciton config.
   * @param state - state of connection config that will be saved in crudable.
   * @return Nothing.
   */
  private fun updateZoweConfigIfNeeded (state: ConnectionDialogState?) {
    val res = showOkCancelDialog(
      title = "Zowe Config Update",
      message = "Update zowe config file?\n${state?.zoweConfigPath}",
      okText = "Yes",
      cancelText = "No"
    )
    if (res == Messages.OK) {
      val zoweConfigPath = state?.zoweConfigPath ?: return
      val configFile = VirtualFileManager.getInstance().findFileByNioPath(java.nio.file.Path.of(zoweConfigPath)) ?: let {
        Messages.showErrorDialog("Zowe config file not found", "Zowe Config")
        return
      }

      val zoweConfig = parseConfigJson(configFile.inputStream)
      zoweConfig.extractSecureProperties(configFile.path.split("/").toTypedArray())
      zoweConfig.updateFromState(state)
      runWriteActionOnWriteThread {
        zoweConfig.saveSecureProperties(configFile.path.split("/").toTypedArray())
        configFile.setBinaryContent(zoweConfig.toJson().toByteArray(configFile.charset))
      }
    }
  }

  private val zoweConfigStates = mutableListOf<ConnectionDialogState>()

  private fun editConnection() {
    val idx = connectionsTable?.selectedRow
    if (idx != null && connectionsTableModel != null) {
      val state = showAndTestConnection(connectionsTableModel!![idx].apply {
        mode = DialogMode.UPDATE
      })
      if (state?.zoweConfigPath != null){
        zoweConfigStates.add(state)
      }
      if (state != null) {
        connectionsTableModel?.set(idx, state)
      }
    }
  }

  private fun removeSelectedConnections() {
    val indices = connectionsTable?.selectedRows
    indices?.forEachIndexed { i, idx ->
      connectionsTableModel?.removeRow(idx - i)
    }
  }

  private fun removeConnectionsWithWarning(selectedConfigs: List<ConnectionDialogState>) {
    val workingSets = sandboxCrudable.getAll<FilesWorkingSetConfig>().toMutableList()
    val wsUsages = workingSets.filter { wsConfig ->
      selectedConfigs.any { state -> wsConfig.connectionConfigUuid == state.connectionConfig.uuid }
    }

    if (wsUsages.isEmpty()) {
      removeSelectedConnections()
      return
    }

    val warningMessageBuilder = StringBuilder("The following working sets use selected connections:\n    ")
    wsUsages.forEach { wsConfig ->
      warningMessageBuilder.append(wsConfig.name).append(", ")
    }
    warningMessageBuilder.setLength(warningMessageBuilder.length - 2)
    warningMessageBuilder.append(".\n\nDo you really want to remove them?")

    val ret = Messages.showOkCancelDialog(
      warningMessageBuilder.toString(),
      "Warning",
      "Ok",
      "Cancel",
      Messages.getWarningIcon()
    )

    if (ret == Messages.OK)
      removeSelectedConnections()
  }

  private var connectionsTableModel: ConnectionsTableModel? = null
  private var connectionsTable: ValidatingTableView<ConnectionDialogState>? = null

  private var panel: DialogPanel? = null

  override fun createPanel(): DialogPanel {
    val tableModel = ConnectionsTableModel(sandboxCrudable)

    connectionsTableModel = tableModel
    val table = ValidatingTableView(tableModel, disposable!!).apply {
      rowHeight = DEFAULT_ROW_HEIGHT
    }

    connectionsTable = table

    ApplicationManager.getApplication()
      .messageBus
      .connect(disposable!!)
      .subscribe(SandboxListener.TOPIC, object : SandboxListener {
        override fun <E : Any> update(clazz: Class<out E>) {
        }

        override fun <E : Any> reload(clazz: Class<out E>) {
          if (clazz.isThe<ConnectionConfig>()) {
            tableModel.reinitialize()
          }
        }
      })

    return panel {
      row {
        cell(isFullWidth = true) {
          toolbarTable("Connections", table) {
            configureDecorator {
              disableUpDownActions()
              setAddAction {
                addConnection()
              }
              setEditAction {
                table.selectedObject?.let {
                  editConnection()
                }
              }
              setEditActionUpdater {
                table.selectedRowCount == 1
              }
              setRemoveAction {
                removeConnectionsWithWarning(table.selectedObjects)
              }
            }
          }
        }
      }
    }.also {
      panel = it
      panel?.updateUI()
      if (openAddDialog) {
        invokeLater {
          addConnection()
          openAddDialog = false
        }
      }
    }
  }

  override fun apply() {
    val wasModified = isModified
    applySandbox<Credentials>()
    applySandbox<ConnectionConfig>()
    zoweConfigStates.distinct().forEach { updateZoweConfigIfNeeded(it) }
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun reset() {
    val wasModified = isModified
    rollbackSandbox<Credentials>()
    rollbackSandbox<ConnectionConfig>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun isModified(): Boolean {
    return isSandboxModified<Credentials>()
        || isSandboxModified<ConnectionConfig>()
  }

  override fun cancel() {
    reset()
  }


}
