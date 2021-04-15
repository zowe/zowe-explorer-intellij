package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.nextUniqueValue

data class WorkingSetDialogState(
  var uuid: String = "",
  var connectionUuid: String = "",
  var workingSetName: String = "",
  var maskRow: MutableList<TableRow> = mutableListOf(),
  override var mode: DialogMode = DialogMode.CREATE
) : DialogState {

  val workingSetConfig
    get() = WorkingSetConfig(
      this.uuid,
      this.workingSetName,
      this.connectionUuid,
      this.maskRow.filter { it.type == TableRow.ZOS }.map { DSMask(it.mask, mutableListOf()) }.toMutableSmartList(),
      this.maskRow.filter { it.type == TableRow.USS }.map { UssPath(it.mask) }.toMutableSmartList()
    )

  class TableRow(
    var mask: String = "",
    var type: String = "z/OS",
    var isSingle: Boolean = false
  ) {
    companion object {
      const val ZOS = "z/OS"
      const val USS = "USS"
    }
  }
}

fun WorkingSetDialogState.initEmptyUuids(crudable: Crudable): WorkingSetDialogState {
  return this.apply {
    uuid = crudable.nextUniqueValue<WorkingSetConfig, String>()
  }
}

fun WorkingSetConfig.toDialogState(): WorkingSetDialogState {
  return WorkingSetDialogState(
    uuid = this.uuid,
    connectionUuid = this.connectionConfigUuid,
    workingSetName = this.name,
    maskRow = this.dsMasks.map { WorkingSetDialogState.TableRow(mask = it.mask) }.plus(this.ussPaths.map {
      WorkingSetDialogState.TableRow(mask = it.path, type = WorkingSetDialogState.TableRow.USS)
    }).toMutableSmartList(),
  )
}