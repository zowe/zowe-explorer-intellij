package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable

abstract class AbstractWsDialogState<WSConfig, TableRow>(
  var uuid: String = "",
  var connectionUuid: String = "",
  var workingSetName: String = "",
  var maskRow: MutableList<TableRow> = mutableListOf(),
  override var mode: DialogMode = DialogMode.CREATE
) : DialogState {

  abstract fun workingSetConfigClass(): Class<out WSConfig>

  abstract val workingSetConfig: WSConfig
}

fun <WSConfig, T : AbstractWsDialogState<WSConfig, *>> T.initEmptyUuids(crudable: Crudable): T {
  return this.apply {
    uuid = crudable.nextUniqueValue<WSConfig, String>(workingSetConfigClass())
  }
}

class WorkingSetDialogState(
  uuid: String = "",
  connectionUuid: String = "",
  workingSetName: String = "",
  maskRow: MutableList<TableRow> = mutableListOf(),
  mode: DialogMode = DialogMode.CREATE
) : AbstractWsDialogState<FilesWorkingSetConfig, WorkingSetDialogState.TableRow>(
  uuid,
  connectionUuid,
  workingSetName,
  maskRow,
  mode
) {
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

  override fun workingSetConfigClass() = FilesWorkingSetConfig::class.java
  override val workingSetConfig: FilesWorkingSetConfig
    get() = FilesWorkingSetConfig(
      this.uuid,
      this.workingSetName,
      this.connectionUuid,
      this.maskRow.filter { it.type == TableRow.ZOS }.map { DSMask(it.mask, mutableListOf()) }.toMutableSmartList(),
      this.maskRow.filter { it.type == TableRow.USS }.map { UssPath(it.mask) }.toMutableSmartList()
    )

}

class JobsWorkingSetDialogState(
  uuid: String = "",
  connectionUuid: String = "",
  workingSetName: String = "",
  maskRow: MutableList<TableRow> = mutableListOf(),
  mode: DialogMode = DialogMode.CREATE
) : AbstractWsDialogState<JobsWorkingSetConfig, JobsWorkingSetDialogState.TableRow>(
  uuid,
  connectionUuid,
  workingSetName,
  maskRow,
  mode
) {

  class TableRow(
    var prefix: String = "",
    var owner: String = "",
    var jobId: String = ""
  ) {
    companion object {
      const val ZOS = "z/OS"
      const val USS = "USS"
    }
  }

  override fun workingSetConfigClass() = JobsWorkingSetConfig::class.java
  override val workingSetConfig: JobsWorkingSetConfig
    get() = JobsWorkingSetConfig(
      this.uuid,
      this.workingSetName,
      this.connectionUuid,
      this.maskRow.map { JobsFilter(it.owner, it.prefix, it.jobId) }.toMutableSmartList()
    )

}
