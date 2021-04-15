package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.common.ui.toolbarTable
import eu.ibagroup.formainframe.config.*
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.isThe

fun noConnectionsInSandbox() = sandboxCrudable.getAll<ConnectionConfig>().count() == 0L

class WSConfigurable(
) : BoundSearchableConfigurable("Working Sets", "mainframe") {

  private var panel: DialogPanel? = null

  private inner class ConnectionSandboxListener(
    private val predicate: ComponentPredicate,
    private val listener: (Boolean) -> Unit
  ): SandboxListener {
    private fun onEvent() {
      listener(predicate())
    }

    override fun <E : Any> update(clazz: Class<out E>) {
      if (clazz.isThe<ConnectionConfig>()) {
        onEvent()
      }
    }

    override fun <E : Any> reload(clazz: Class<out E>) {
      update(clazz)
    }
  }



  override fun createPanel(): DialogPanel {
    val wsTableModel = WSTableModel(sandboxCrudable)
    val wsTable = ValidatingTableView(wsTableModel, disposable!!).apply {
      rowHeight = DEFAULT_ROW_HEIGHT
    }

    ApplicationManager.getApplication()
      .messageBus
      .connect(disposable!!)
      .subscribe(SandboxListener.TOPIC, object : SandboxListener {
        override fun <E : Any> update(clazz: Class<out E>) {
        }
        override fun <E : Any> reload(clazz: Class<out E>) {
          if (clazz.isThe<WorkingSetConfig>()) {
            wsTableModel.reinitialize()
          }
        }
      })
    return panel {
      row {
        cell(isVerticalFlow = true, isFullWidth = false) {
//          ActionLink("There is no defined Connections to create a Working Set with") {
//            goToAddConnection()
//          }()
//            .enableIf(ActionLinkPredicate(disposable!!))
//            .applyIfEnabled()
          toolbarTable(message("configurable.ws.tables.ws.title"), wsTable) {
            addNewItemProducer { WorkingSetConfig() }
            configureDecorator {
              disableUpDownActions()
              setAddAction {
                WorkingSetConfig().toDialogState().initEmptyUuids(sandboxCrudable).let { s ->
                  WorkingSetDialog(sandboxCrudable, s)
                    .apply {
                      if (showAndGet()) {
                        wsTableModel.addRow(state.workingSetConfig)
                      }
                    }
                }
              }
              setEditAction {
                wsTable.selectedObject?.let { selected ->
                  WorkingSetDialog(sandboxCrudable, selected.toDialogState().apply { mode = DialogMode.UPDATE }).apply {
                    if (showAndGet()) {
                      val idx = wsTable.selectedRow
                      wsTableModel[idx] = state.workingSetConfig
                    }
                  }
                }
              }
              setToolbarPosition(ActionToolbarPosition.BOTTOM)
            }
          }
        }
      }
    }.also {
      panel = it
    }
  }

  override fun apply() {
    val wasModified = isModified
    applySandbox<WorkingSetConfig>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun isModified(): Boolean {
    return isSandboxModified<WorkingSetConfig>()
  }

  override fun reset() {
    val wasModified = isModified
    rollbackSandbox<WorkingSetConfig>()
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun cancel() {
    reset()
  }

}