/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
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
import eu.ibagroup.formainframe.config.ConfigSandbox
import eu.ibagroup.formainframe.config.SandboxListener
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.isThe
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Abstract class for Working Set configurables.
 * @param WSConfig - implementation of WorkingSetConfig class.
 * @see WorkingSetConfig
 * @param WSModel - implementation of table model to store Working Set
 *                  List UI data (columns, e.g. name, connection ..., and list of rows (working sets)).
 * @see CrudableTableModel
 * @param DState - Working Set dialog state.
 * @author Valiantsin Krus
 * @author Viktar Mushtsin
 */
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

  /**
   * Creates initial unique identifier (uuid) for DState class
   * @param crudable - Crudable instance that contains UniqueValue provider.
   * @return this DState object with updated uuid.
   */
  private fun DState.initEmptyUuids(crudable: Crudable): DState {
    return this.apply {
      uuid = crudable.nextUniqueValue<WSConfig, String>(wsConfigClass)
    }
  }

  /**
   * Registers custom mouse listeners for the specified WS table view (Files WS or JES WS)
   * @param wsTable - working set table view instance
   * @return An instance of MouseAdapter
   */
  private fun registerMouseListeners(wsTable: ValidatingTableView<WSConfig>): MouseAdapter = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount == 2) {
        wsTable.selectedObject?.let { selected ->
          createEditDialog(selected.toDialogStateAbstract())
        }
      }
    }
  }

  /**
   * Creates panel with table containing Working Set List inside.
   * @return formed DialogPanel.
   */
  override fun createPanel(): DialogPanel {
    wsTable = ValidatingTableView(wsTableModel, disposable!!).apply {
      rowHeight = DEFAULT_ROW_HEIGHT
      addMouseListener(registerMouseListeners(this))
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
                emptyConfig()
                  .toDialogStateAbstract()
                  .initEmptyUuids(ConfigSandbox.getService().crudable)
                  .let { s ->
                    createAddDialog(ConfigSandbox.getService().crudable, s)
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

  /**
   * Saves working sets configurations in config service.
   */
  override fun apply() {
    val wasModified = isModified
    ConfigSandbox.getService().apply(wsConfigClass)
    if (wasModified) {
      panel?.updateUI()
    }
  }

  /**
   * Compares state of config sandbox with config service.
   * @return true if states are equal and false otherwise.
   */
  override fun isModified(): Boolean {
    return ConfigSandbox.getService().isModified(wsConfigClass)
  }

  /**
   * Discards changes in config sandbox.
   */
  override fun reset() {
    val wasModified = isModified
    ConfigSandbox.getService().rollback(wsConfigClass)
    if (wasModified) {
      panel?.updateUI()
    }
  }

  /**
   * Does the same as reset method.
   */
  override fun cancel() {
    reset()
  }
}
