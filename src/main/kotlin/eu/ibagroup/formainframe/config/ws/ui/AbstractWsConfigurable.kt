package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.*
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.isThe


abstract class AbstractWsConfigurable<WSConfig, WSModel: CrudableTableModel<WSConfig>, DState: AbstractWsDialogState<WSConfig, *>>(
  displayName: String
): BoundSearchableConfigurable(displayName, "mainframe") {

  abstract val wsConfigClass: Class<out WSConfig>

  abstract val wsTableModel: WSModel

  val wsTable by lazy {
    ValidatingTableView(wsTableModel, disposable!!).apply {
      rowHeight = DEFAULT_ROW_HEIGHT
    }
  }

  private var panel: DialogPanel? = null

  abstract fun emptyConfig(): WSConfig

  abstract fun WSConfig.toDialogStateAbstract(): DState

  abstract fun createAddDialog(crudable: Crudable, state: DState)

  abstract fun createEditDialog(selected: DState)

  private fun DState.initEmptyUuids(crudable: Crudable): DState {
    return this.apply {
      uuid = crudable.nextUniqueValue<WSConfig, String>(wsConfigClass)
    }
  }

  override fun createPanel(): DialogPanel {
//    val wsTableModel = WSTableModel(sandboxCrudable)

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
          if (clazz.isThe(wsConfigClass)) {
            wsTableModel.reinitialize()
          }
        }
      })
    return panel {
      row {
        cell(isVerticalFlow = true, isFullWidth = false) {
          toolbarTable(displayName, wsTable) {
            addNewItemProducer { emptyConfig() }
            configureDecorator {
              disableUpDownActions()
              setAddAction {
                emptyConfig().toDialogStateAbstract().initEmptyUuids(sandboxCrudable).let { s ->
//                  WorkingSetDialog(sandboxCrudable, s)
//                    .apply {
//                      if (showAndGet()) {
//                        wsTableModel.addRow(state.workingSetConfig)
//                        wsTableModel.reinitialize()
//                      }
//                    }
                  createAddDialog(sandboxCrudable, s)
                }
              }
              setEditAction {
                wsTable.selectedObject?.let { selected ->
                  createEditDialog(selected.toDialogStateAbstract())
//                  WorkingSetDialog(sandboxCrudable, selected.toDialogState().apply { mode = DialogMode.UPDATE }).apply {
//                    if (showAndGet()) {
//                      val idx = wsTable.selectedRow
//                      wsTableModel[idx] = state.workingSetConfig
//                      wsTableModel.reinitialize()
//                    }
//                  }
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
    applySandbox(wsConfigClass)
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun isModified(): Boolean {
    return isSandboxModified(wsConfigClass)
  }

  override fun reset() {
    val wasModified = isModified
    rollbackSandbox(wsConfigClass)
    if (wasModified) {
      panel?.updateUI()
    }
  }

  override fun cancel() {
    reset()
  }
}
