/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.common.ui.CrudableTableModel
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.common.ui.tableWithToolbar
import eu.ibagroup.formainframe.config.*
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.isThe

// TODO: doc Valiantsin
abstract class AbstractWsConfigurable<WSConfig : WorkingSetConfig, WSModel : CrudableTableModel<WSConfig>, DState : AbstractWsDialogState<WSConfig, *>>(
  displayName: String
) : BoundSearchableConfigurable(displayName, "mainframe") {

  abstract val wsConfigClass: Class<out WSConfig>

  abstract val wsTableModel: WSModel

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
      group(displayName, false) {
        row {
          tableWithToolbar(wsTable) {
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
          .resizableRow()
      }
        .resizableRow()
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
