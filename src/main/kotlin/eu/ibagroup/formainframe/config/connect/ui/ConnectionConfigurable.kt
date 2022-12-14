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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.common.ui.tableWithToolbar
import eu.ibagroup.formainframe.config.*
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.isThe
import eu.ibagroup.formainframe.utils.toMutableList

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

  /** Dialog to edit existing connection. Triggers the dialog and, after the changes, tests the connection and adds the changes */
  private fun editConnection() {
    val idx = connectionsTable?.selectedRow
    if (idx != null && connectionsTableModel != null) {
      val state = showAndTestConnection(
        connectionsTableModel!![idx].apply { mode = DialogMode.UPDATE }
      )
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
    val warningMessageBuilder = StringBuilder("<nobr>The following $wsType working sets use selected connections:</nobr><br>")
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
