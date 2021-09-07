package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.*
import eu.ibagroup.formainframe.config.*
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.isThe


abstract class AbstractWsConfigurable<WSConfig: WorkingSetConfig, WSModel : CrudableTableModel<WSConfig>, DState : AbstractWsDialogState<WSConfig, *>>(
  displayName: String
) : BoundSearchableConfigurable(displayName, "mainframe") {

  abstract val wsConfigClass: Class<out WSConfig>

  abstract val wsTableModel: WSModel

//  val wsTable = ValidatingTableView(wsTableModel, disposable!!).apply {
//    rowHeight = DEFAULT_ROW_HEIGHT
//  }
  lateinit var wsTable: ValidatingTableView<WSConfig>

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

    wsTable = ValidatingTableView(wsTableModel, disposable!!).apply {
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
                  createAddDialog(sandboxCrudable, s)
                }
              }
              setEditAction {
                wsTable.selectedObject?.let { selected ->
                  createEditDialog(selected.toDialogStateAbstract())
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
