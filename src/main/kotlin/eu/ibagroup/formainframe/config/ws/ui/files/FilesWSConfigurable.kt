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

package eu.ibagroup.formainframe.config.ws.ui.files

import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.config.ConfigSandbox
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.MaskState
import eu.ibagroup.formainframe.config.ws.ui.AbstractWsConfigurable
import eu.ibagroup.formainframe.config.ws.ui.FilesWorkingSetDialogState
import eu.ibagroup.formainframe.utils.MaskType
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Implementation of [AbstractWsConfigurable] class for modifying Files Working Set configurations.
 */
class FilesWSConfigurable :
  AbstractWsConfigurable<FilesWorkingSetConfig, WSTableModel, FilesWorkingSetDialogState>("Working Sets") {
  override val wsConfigClass = FilesWorkingSetConfig::class.java
  override val wsTableModel = WSTableModel(ConfigSandbox.getService().crudable)

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
    FilesWorkingSetDialog(ConfigSandbox.getService().crudable, state)
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
      ConfigSandbox.getService().crudable,
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
