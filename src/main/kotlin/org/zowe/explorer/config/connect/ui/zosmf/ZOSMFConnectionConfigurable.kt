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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.progress.runBackgroundableTask
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
import org.zowe.explorer.utils.runWriteActionInEdtAndWait
import org.zowe.explorer.utils.toMutableList
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import kotlin.collections.set

/** Create and manage Connections tab in settings */
@Suppress("DialogTitleCapitalization")
class ZOSMFConnectionConfigurable : BoundSearchableConfigurable("z/OSMF Connections", "mainframe") {

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
      message = "Do you want to update zowe config file?\n${state?.zoweConfigPath}",
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
      runWriteActionInEdtAndWait {
        zoweConfig.saveSecureProperties(configFile.path.split("/").toTypedArray())
        configFile.setBinaryContent(zoweConfig.toJson().toByteArray(configFile.charset))
      }
    }
  }

  private val zoweConfigStates = hashMapOf<String, ConnectionDialogState>()

  /** Dialog to edit existing connection. Triggers the dialog and, after the changes, tests the connection and adds the changes */
  private fun editConnection() {
    val idx = connectionsTable?.selectedRow
    if (idx != null && connectionsTableModel != null) {
      val state = showAndTestConnection(connectionsTableModel!![idx].apply {
        mode = DialogMode.UPDATE
      })
      state?.zoweConfigPath?.let {
        zoweConfigStates[it] = state
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

  companion object {
    /**
     * Generates a connection removal warning message that is used for Files/JES working set
     * @param wsUsages - list of working sets for the current connection
     * @param wsType - working set type
     * @return StringBuilder with result string.
     */
    fun generateRemoveWarningMessage(wsUsages: List<WorkingSetConfig>, wsType: String): StringBuilder {
      val warningMessageBuilder =
        StringBuilder("<nobr>The following $wsType working sets use selected connections:</nobr><br>")
      wsUsages.forEach { wsConfig ->
        warningMessageBuilder.append(wsConfig.name).append(", ")
      }
      warningMessageBuilder.setLength(warningMessageBuilder.length - 2)
      warningMessageBuilder.append(".<br>")
      return warningMessageBuilder
    }

    /**
     * Creates a message dialog when a connection is deleted if it is used in any working set
     * @param filesWsUsages - list of files working sets
     * @param jesWsUsages - list of JES working sets
     * @return number of button pressed
     */
    fun warningMessageForDeleteConfig(
      filesWsUsages: List<FilesWorkingSetConfig>,
      jesWsUsages: List<JesWorkingSetConfig>
    ): Int {
      val warningMessageBuilder = StringBuilder()
      if (filesWsUsages.isNotEmpty()) {
        warningMessageBuilder.append(generateRemoveWarningMessage(filesWsUsages, "Files"))
      }
      if (jesWsUsages.isNotEmpty()) {
        warningMessageBuilder.append(generateRemoveWarningMessage(jesWsUsages, "JES"))
      }
      warningMessageBuilder.append("<br>Do you really want to remove it?")

      return Messages.showOkCancelDialog(
        warningMessageBuilder.toString(),
        "Warning",
        "Yes",
        "Cancel",
        Messages.getWarningIcon()
      )
    }
  }

  /** Remove connections with the warning before they are deleted */
  private fun removeConnectionsWithWarning(selectedConfigs: List<ConnectionDialogState>) {

    // TODO: Find working sets for connection using templated way without specific implementation.
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

    val ret = warningMessageForDeleteConfig(filesWsUsages, jesWsUsages)

    if (ret == Messages.OK)
      removeSelectedConnections()
  }

  private var connectionsTableModel: ConnectionsTableModel? = null
  private var connectionsTable: ValidatingTableView<ConnectionDialogState>? = null

  private var panel: DialogPanel? = null

  /**
   * Registers custom mouse listeners for the connections table view
   * @param connectionsTable - connections table view object
   * @return An instance of MouseAdapter
   */
  private fun registerMouseListeners(connectionsTable: ValidatingTableView<ConnectionDialogState>): MouseAdapter =
    object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          connectionsTable.selectedObject?.let {
            editConnection()
          }
        }
      }
    }

  /** Create Connections panel in settings */
  override fun createPanel(): DialogPanel {
    val tableModel = ConnectionsTableModel(sandboxCrudable)

    connectionsTableModel = tableModel
    val table = ValidatingTableView(tableModel, disposable!!)
      .apply {
        rowHeight = DEFAULT_ROW_HEIGHT
        addMouseListener(registerMouseListeners(this))
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
      group("z/OSMF Connections", false) {
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
          runInEdt {
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
    zoweConfigStates.values.forEach {
      if (isModified) {
        updateZoweConfigIfNeeded(it)
      }
    }
    if (wasModified) {
      panel?.updateUI()
    }
    zoweConfigStates.clear()
  }

  /** Reset the Connections table changes. Updates UI when the changes were introduced */
  override fun reset() {
    runBackgroundableTask(title = "Reset changes", cancellable = false) {
      val wasModified = isModified
      rollbackSandbox<Credentials>()
      rollbackSandbox<ConnectionConfig>()
      if (wasModified) {
        panel?.updateUI()
      }
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
