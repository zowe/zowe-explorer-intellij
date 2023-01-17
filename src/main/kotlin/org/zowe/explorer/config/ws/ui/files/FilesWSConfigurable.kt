/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui.files

import com.intellij.util.containers.toMutableSmartList
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.sandboxCrudable
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.MaskState
import org.zowe.explorer.config.ws.ui.AbstractWsConfigurable
import org.zowe.explorer.config.ws.ui.FilesWorkingSetDialogState
import org.zowe.explorer.utils.MaskType
import org.zowe.explorer.utils.crudable.Crudable

/**
 * Implementation of AbstractWsConfigurable class for modifying Files Working Set configurations.
 * @see AbstractWsConfigurable
 */
class FilesWSConfigurable :
  AbstractWsConfigurable<FilesWorkingSetConfig, WSTableModel, FilesWorkingSetDialogState>("Working Sets") {
  override val wsConfigClass = FilesWorkingSetConfig::class.java
  override val wsTableModel = WSTableModel(sandboxCrudable)

  override fun emptyConfig() = FilesWorkingSetConfig()

  /**
   * Creates FilesWorkingSetDialogState based on data of FilesWorkingSetConfig.
   */
  override fun FilesWorkingSetConfig.toDialogStateAbstract() = this.toDialogState()

  /**
   * Creates and shows dialog for adding Files Working Set.
   * @param crudable crudable to modify after applying dialog.
   * @param state state of dialog.
   */
  override fun createAddDialog(crudable: Crudable, state: FilesWorkingSetDialogState) {
    FilesWorkingSetDialog(sandboxCrudable, state)
      .apply {
        if (showAndGet()) {
          wsTableModel.addRow(state.workingSetConfig)
          wsTableModel.reinitialize()
        }
      }
  }

  /**
   * Creates and shows dialog for editing Files Working Set.
   * @param selected dialog state of selected working set to edit
   */
  override fun createEditDialog(selected: FilesWorkingSetDialogState) {
    FilesWorkingSetDialog(
      sandboxCrudable,
      selected.apply { mode = DialogMode.UPDATE })
      .apply {
        if (showAndGet()) {
          val idx = wsTable.selectedRow
          wsTableModel[idx] = state.workingSetConfig
          wsTableModel.reinitialize()
        }
      }
  }

}

/**
 * Creates FilesWorkingSetDialogState based on data of FilesWorkingSetConfig.
 */
fun FilesWorkingSetConfig.toDialogState(): FilesWorkingSetDialogState {
  return FilesWorkingSetDialogState(
    uuid = this.uuid,
    connectionUuid = this.connectionConfigUuid,
    workingSetName = this.name,
    maskRow = this.dsMasks
      .map { MaskState(mask = it.mask) }
      .plus(this.ussPaths.map {
        MaskState(
          mask = it.path,
          type = MaskType.USS
        )
      })
      .toMutableSmartList(),
  )
}
