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

package eu.ibagroup.formainframe.tso.config.ui

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.common.ui.DEFAULT_ROW_HEIGHT
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.common.ui.tableWithToolbar
import eu.ibagroup.formainframe.config.ConfigSandbox
import eu.ibagroup.formainframe.config.SandboxListener
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.tso.config.ui.table.TSOSessionTableModel
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.isThe
import eu.ibagroup.formainframe.utils.subscribe
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Create and manage TSO Sessions tab in settings
 */
class TSOSessionConfigurable : BoundSearchableConfigurable("TSO Sessions", "mainframe") {

  private lateinit var tableModel: TSOSessionTableModel
  private lateinit var table: ValidatingTableView<TSOSessionDialogState>
  private lateinit var panel: DialogPanel

  /**
   * Create TSO Sessions panel in settings
   */
  override fun createPanel(): DialogPanel {
    tableModel = TSOSessionTableModel(ConfigSandbox.getService().crudable)
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
    ConfigSandbox.getService().apply(TSOSessionConfig::class.java)
    if (wasModified) {
      panel.updateUI()
    }
  }

  /**
   * Reset the TSO Sessions table changes. Updates UI when the changes were introduced
   */
  override fun reset() {
    val wasModified = isModified
    ConfigSandbox.getService().rollback(TSOSessionConfig::class.java)
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
    return ConfigSandbox.getService().isModified(TSOSessionConfig::class.java)
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
    val state = TSOSessionDialogState().initEmptyUuids(ConfigSandbox.getService().crudable)
    val dialog = TSOSessionDialog(ConfigSandbox.getService().crudable, state)
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
      val dialog = TSOSessionDialog(ConfigSandbox.getService().crudable, state)
      if (dialog.showAndGet()) {
        val idx = table.selectedRow
        tableModel[idx] = state
      }
    }
  }
}