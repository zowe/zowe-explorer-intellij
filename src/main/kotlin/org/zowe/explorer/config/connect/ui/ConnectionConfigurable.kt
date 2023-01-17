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
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.common.ui.DEFAULT_ROW_HEIGHT
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.ValidatingTableView
import org.zowe.explorer.common.ui.tableWithToolbar
import org.zowe.explorer.config.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.isThe
import org.zowe.explorer.utils.runWriteActionOnWriteThread
import org.zowe.explorer.utils.toMutableList
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.net.URI

/** Create and manage Connections tab in settings */
@Suppress("DialogTitleCapitalization")
class ConnectionConfigurable : BoundSearchableConfigurable("z/OSMF Connections", "mainframe") {

  var openAddDialog = false

  /**
   * Dialog that shows the connection attempt
   * @param initialState the initial state of the dialog
   */
  private fun showAndTestConnection(initialState: ConnectionDialogState = ConnectionDialogState()): ConnectionDialogState? {
    return ConnectionDialog.showAndTestConnection(
      crudable = sandboxCrudable,
      parentComponent = panel?.components?.getOrNull(0),
      initialState = initialState
    )
  }

  /** Dialog to add connection. Triggers the dialog and adds the new row to the Connections table in case the new connection is added */
  private fun addConnection() {
    showAndTestConnection()?.let { connectionsTableModel?.addRow(it) }
  }

  private fun ZoweConfig.updateFromState(state: ConnectionDialogState) {
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
  private fun updateZoweConfigIfNeeded(state: ConnectionDialogState?) {
    val res = showOkCancelDialog(
      title = "Zowe Config Update",
      message = "Update zowe config file?\n${state?.zoweConfigPath}",
      okText = "Yes",
      cancelText = "No"
    )
    if (res == Messages.OK) {
      val zoweConfigPath = state?.zoweConfigPath ?: return
      val configFile =
        VirtualFileManager.getInstance().findFileByNioPath(java.nio.file.Path.of(zoweConfigPath)) ?: let {
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

  /** Dialog to edit existing connection. Triggers the dialog and, after the changes, tests the connection and adds the changes */
  private fun editConnection() {
    val idx = connectionsTable?.selectedRow
    if (idx != null && connectionsTableModel != null) {
      val state = showAndTestConnection(connectionsTableModel!![idx].apply {
        mode = DialogMode.UPDATE
      })
      if (state?.zoweConfigPath != null) {
        zoweConfigStates.add(state)
      }
      if (state != null) {
        connectionsTableModel?.set(idx, state)
      }
    }
  }

  /** Delete selected connections from Connections table */
  private fun removeSelectedConnections() {
    val indices = connectionsTable?.selectedRows
    indices?.forEachIndexed { i, idx ->
      connectionsTableModel?.removeRow(idx - i)
    }
  }

  /** Generates a connection removal warning message that is used for working sets */
  private fun generateRemoveWarningMessage(wsUsages: List<WorkingSetConfig>, wsType: String): StringBuilder {
    val warningMessageBuilder =
      StringBuilder("<nobr>The following $wsType working sets use selected connections:</nobr><br>")
    wsUsages.forEach { wsConfig ->
      warningMessageBuilder.append(wsConfig.name).append(", ")
    }
    warningMessageBuilder.setLength(warningMessageBuilder.length - 2)
    warningMessageBuilder.append(".<br>")
    return warningMessageBuilder
  }

  /** Remove connections with the warning before they are deleted */
  private fun removeConnectionsWithWarning(selectedConfigs: List<ConnectionDialogState>) {
    val filesWorkingSets = sandboxCrudable.getAll<FilesWorkingSetConfig>().toMutableList()
    val filesWsUsages = filesWorkingSets.filter { filesWsConfig ->
      selectedConfigs.any { state -> filesWsConfig.connectionConfigUuid == state.connectionConfig.uuid }
    }

    val jesWorkingSet = sandboxCrudable.getAll<JesWorkingSetConfig>().toMutableList()
    val jesWsUsages = jesWorkingSet.filter { jesWsConfig ->
      selectedConfigs.any { state -> jesWsConfig.connectionConfigUuid == state.connectionConfig.uuid }
    }

    if (filesWsUsages.isEmpty() && jesWsUsages.isEmpty()) {
      removeSelectedConnections()
      return
    }

    val warningMessageBuilder = StringBuilder()
    if (filesWsUsages.isNotEmpty()) {
      warningMessageBuilder.append(generateRemoveWarningMessage(filesWsUsages, "Files"))
    }
    if (jesWsUsages.isNotEmpty()) {
      warningMessageBuilder.append(generateRemoveWarningMessage(jesWsUsages, "JES"))
    }
    warningMessageBuilder.append("<br>Do you really want to remove it?")

    val ret = Messages.showOkCancelDialog(
      warningMessageBuilder.toString(),
      "Warning",
      "Yes",
      "Cancel",
      Messages.getWarningIcon()
    )

    if (ret == Messages.OK)
      removeSelectedConnections()
  }

  private var connectionsTableModel: ConnectionsTableModel? = null
  private var connectionsTable: ValidatingTableView<ConnectionDialogState>? = null

  private var panel: DialogPanel? = null

  /** Create Connections panel in settings */
  override fun createPanel(): DialogPanel {
    val tableModel = ConnectionsTableModel(sandboxCrudable)

    connectionsTableModel = tableModel
    val table = ValidatingTableView(tableModel, disposable!!)
      .apply {
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
      group("Connections", false) {
        row {
          tableWithToolbar(table) {
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
          .resizableRow()
      }
        .resizableRow()
    }
      .also {
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

  /** Apply the Connections table changes. Updates UI when the changes were introduced */
  override fun apply() {
    val wasModified = isModified
    applySandbox<Credentials>()
    applySandbox<ConnectionConfig>()
    zoweConfigStates.distinct().forEach { updateZoweConfigIfNeeded(it) }
    if (wasModified) {
      panel?.updateUI()
    }
  }

  /** Reset the Connections table changes. Updates UI when the changes were introduced */
  override fun reset() {
    val wasModified = isModified
    rollbackSandbox<Credentials>()
    rollbackSandbox<ConnectionConfig>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  /** Check are the Credentials and Connections sandboxes modified */
  override fun isModified(): Boolean {
    return isSandboxModified<Credentials>()
      || isSandboxModified<ConnectionConfig>()
  }

  override fun cancel() {
    reset()
  }

}
