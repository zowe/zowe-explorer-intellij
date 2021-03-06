/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui

import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel
import org.zowe.explorer.common.ui.*
import org.zowe.explorer.config.*
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.isThe


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
