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

import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.config.ws.*
import eu.ibagroup.formainframe.utils.MaskType
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Abstract class for Working Sets state in configuration dialogs (e.g. Files Working Set, JES Working Sets)
 * @param WSConfig WorkingSetConfig implementation class.
 * @see WorkingSetConfig
 * @see FilesWorkingSetConfig
 * @see JesWorkingSetConfig
 * @author Valiantsin Krus
 */
abstract class AbstractWsDialogState<WSConfig : WorkingSetConfig, TableRow>(
  var uuid: String = "",
  var connectionUuid: String = "",
  var workingSetName: String = "",
  var maskRow: MutableList<TableRow> = mutableListOf(),
  override var mode: DialogMode = DialogMode.CREATE
) : DialogState {

  abstract fun workingSetConfigClass(): Class<out WSConfig>

  abstract val workingSetConfig: WSConfig
}

fun <WSConfig : Any, T : AbstractWsDialogState<WSConfig, *>> T.initEmptyUuids(crudable: Crudable): T {
  return this.apply {
    uuid = crudable.nextUniqueValue(workingSetConfigClass())
  }
}

/**
 * Dialog state for Files Working Set configuration dialog.
 * @see AbstractWsDialogState
 */
class FilesWorkingSetDialogState(
  uuid: String = "",
  connectionUuid: String = "",
  workingSetName: String = "",
  maskRow: MutableList<MaskState> = mutableListOf(),
  mode: DialogMode = DialogMode.CREATE
) : AbstractWsDialogState<FilesWorkingSetConfig, MaskState>(
  uuid,
  connectionUuid,
  workingSetName,
  maskRow,
  mode
) {

  override fun workingSetConfigClass() = FilesWorkingSetConfig::class.java
  override val workingSetConfig: FilesWorkingSetConfig
    get() = FilesWorkingSetConfig(
      this.uuid,
      this.workingSetName,
      this.connectionUuid,
      this.maskRow.filter { it.type == MaskType.ZOS }.map { DSMask(it.mask, mutableListOf()) }.toMutableSmartList(),
      this.maskRow.filter { it.type == MaskType.USS }.map { UssPath(it.mask) }.toMutableSmartList()
    )

}

/**
 * Dialog state for JES Working Set configuration dialog.
 * @see AbstractWsDialogState
 */
class JesWorkingSetDialogState(
  uuid: String = "",
  connectionUuid: String = "",
  workingSetName: String = "",
  maskRow: MutableList<JobFilterState> = mutableListOf(),
  mode: DialogMode = DialogMode.CREATE
) : AbstractWsDialogState<JesWorkingSetConfig, JobFilterState>(
  uuid,
  connectionUuid,
  workingSetName,
  maskRow,
  mode
) {

  override fun workingSetConfigClass() = JesWorkingSetConfig::class.java
  override val workingSetConfig: JesWorkingSetConfig
    get() = JesWorkingSetConfig(
      this.uuid,
      this.workingSetName,
      this.connectionUuid,
      this.maskRow.map { JobsFilter(it.owner, it.prefix, it.jobId) }.toMutableSmartList()
    )

}
