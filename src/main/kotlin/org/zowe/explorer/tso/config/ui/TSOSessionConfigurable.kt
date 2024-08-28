/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.tso.config.ui

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.common.ui.DEFAULT_ROW_HEIGHT
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.ValidatingTableView
import org.zowe.explorer.common.ui.tableWithToolbar
import org.zowe.explorer.config.*
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.tso.config.ui.table.TSOSessionTableModel
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.isThe
import org.zowe.explorer.utils.subscribe
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Create and manage TSO Sessions tab in settings
 */
class TSOSessionConfigurable: BoundSearchableConfigurable("TSO Sessions", "mainframe") {

  private lateinit var tableModel: TSOSessionTableModel
  private lateinit var table: ValidatingTableView<TSOSessionDialogState>
  private lateinit var panel: DialogPanel

  /**
   * Create TSO Sessions panel in settings
   */
  override fun createPanel(): DialogPanel {
    tableModel = TSOSessionTableModel(sandboxCrudable)
    table = ValidatingTableView(tableModel, disposable!!)
      .apply {
        rowHeight = DEFAULT_ROW_HEIGHT
        addMouseListener(registerMouseListener())
      }

    addSandboxListener()

    return panel {
      group("TSO Sessions", false) {
        row {
          tableWithToolbar(table) {
            configureDecorator {
              setAddAction {
                addSession()
              }
              setEditAction {
                editSession()
              }
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
   * Apply the TSO Sessions table changes. Updates UI when the changes were introduced
   */
  override fun apply() {
    val wasModified = isModified
    applySandbox<TSOSessionConfig>()
    if (wasModified) {
      panel.updateUI()
    }
  }

  /**
   * Reset the TSO Sessions table changes. Updates UI when the changes were introduced
   */
  override fun reset() {
    val wasModified = isModified
    rollbackSandbox<TSOSessionConfig>()
    if (wasModified) {
      panel.updateUI()
    }
  }

  /**
   * Cancel the TSO Sessions table changes. Calls reset
   */
  override fun cancel() {
    reset()
  }

  /**
   * Check is the TSO Sessions sandbox modified
   */
  override fun isModified(): Boolean {
    return isSandboxModified<TSOSessionConfig>()
  }

  /**
   * Register custom mouse listener for the TSO session table view
   * @return an instance of [MouseAdapter]
   */
  private fun registerMouseListener(): MouseAdapter = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (e.clickCount == 2) {
        editSession()
      }
    }
  }

  /**
   * Add sandbox listener for update and reload events
   */
  private fun addSandboxListener() {
    subscribe(
      topic = SandboxListener.TOPIC,
      handler = object : SandboxListener {
        override fun <E : Any> update(clazz: Class<out E>) {
        }

        override fun <E : Any> reload(clazz: Class<out E>) {
          if (clazz.isThe<TSOSessionConfig>()) {
            tableModel.reinitialize()
          }
        }

      },
      disposable = disposable!!
    )
  }

  /**
   * Create and show dialog to add TSO session. Also add the new row to the TSO Sessions table model
   */
  private fun addSession() {
    val state = TSOSessionDialogState().initEmptyUuids(sandboxCrudable)
    val dialog = TSOSessionDialog(sandboxCrudable, state)
    if (dialog.showAndGet()) {
      tableModel.addRow(state)
    }
  }

  /**
   * Create and show dialog to edit TSO session. Also update the row in the TSO Sessions table model
   */
  private fun editSession() {
    table.selectedObject?.clone()?.let { state ->
      state.mode = DialogMode.UPDATE
      val dialog = TSOSessionDialog(sandboxCrudable, state)
      if (dialog.showAndGet()) {
        val idx = table.selectedRow
        tableModel[idx] = state
      }
    }
  }
}
