package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.DialogMode
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
import java.lang.StringBuilder

class ConnectionConfigurable : BoundSearchableConfigurable("z/OSMF Connections", "mainframe") {

  var openAddDialog = false

  private fun addConnection() {

    val state = ShowAndTestConnection(
      ConnectionDialogState(),
      sandboxCrudable,
      ProjectManager.getInstance().defaultProject
    ).showUntilTested()
    state?.let {
      connectionsTableModel?.addRow(it)
    }

  }

  private fun editConnection(selected: ConnectionDialogState) {

    val state = ShowAndTestConnection(
      selected,
      sandboxCrudable,
      ProjectManager.getInstance().defaultProject
    ).showUntilTested()

    val idx = connectionsTable?.selectedRow
    if (idx != null && state != null) {
      connectionsTableModel?.set(idx, state)
    }
  }

  private fun removeSelectedConnection() {
    val idx = connectionsTable?.selectedRow
    if (idx != null) {
      connectionsTableModel?.removeRow(idx)
    }
  }

  private fun removeConnectionWithWarning(selected: ConnectionDialogState) {
    val workingSets = sandboxCrudable.getAll<WorkingSetConfig>().toMutableList()
    val wsUsages = workingSets.filter { wsConfig -> wsConfig.connectionConfigUuid == selected.connectionConfig.uuid }

    if (wsUsages.isEmpty()){
      removeSelectedConnection()
      return
    }

    val warningMessageBuilder = StringBuilder("The following working sets use this connection: ")
    wsUsages.forEach {
        wsConfig -> warningMessageBuilder.append(wsConfig.name).append(", ")
    }
    warningMessageBuilder.setLength(warningMessageBuilder.length-2)
    warningMessageBuilder.append("\nDo you really want to remove it?")

    val ret = Messages.showOkCancelDialog(
      warningMessageBuilder.toString(),
      "Warning",
      "Ok",
      "Cancel",
      Messages.getWarningIcon()
    )

    if(ret == Messages.OK)
      removeSelectedConnection()
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
                table.selectedObject?.let { selected ->
                  editConnection(selected.clone().apply { mode = DialogMode.UPDATE });
                }
              }
              setEditActionUpdater {
                table.selectedRowCount == 1
              }
              setRemoveAction {
                table.selectedObject?.let { selected ->
                  removeConnectionWithWarning(selected.clone())
                }
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