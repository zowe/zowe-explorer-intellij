package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.common.ui.toolbarTable
import eu.ibagroup.formainframe.config.*
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.isThe
import eu.ibagroup.formainframe.utils.toMutableList

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

  private fun editConnection() {
    val idx = connectionsTable?.selectedRow
    if (idx != null && connectionsTableModel != null) {
      val state = showAndTestConnection(connectionsTableModel!![idx])
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
    val workingSets = sandboxCrudable.getAll<WorkingSetConfig>().toMutableList()
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
    applySandbox<UrlConnection>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun reset() {
    val wasModified = isModified
    rollbackSandbox<Credentials>()
    rollbackSandbox<ConnectionConfig>()
    rollbackSandbox<UrlConnection>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun isModified(): Boolean {
    return isSandboxModified<Credentials>()
        || isSandboxModified<ConnectionConfig>()
        || isSandboxModified<UrlConnection>()
  }

  override fun cancel() {
    reset()
  }


}