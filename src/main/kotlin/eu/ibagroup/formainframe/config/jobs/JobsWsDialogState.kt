package eu.ibagroup.formainframe.config.jobs

import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.nextUniqueValue

class JobsWsDialogState(
  var uuid: String = "",
  var connectionUuid: String = "",
  var workingSetName: String = "",
  var maskRow: MutableList<TableRow> = mutableListOf(),
  override var mode: DialogMode = DialogMode.CREATE
): DialogState {


  val jobsWorkingSetConfig
    get() = JobsWorkingSetConfig(
      this.uuid,
      this.workingSetName,
      this.connectionUuid,
      this.maskRow.map { JobsFilter(it.owner, it.prefix, it.jobId) }.toMutableSmartList()
    )

  class TableRow(
    var prefix: String = "*",
    var owner: String = "*",
    var jobId: String = "*"
  ) {
    companion object {
      const val ZOS = "z/OS"
      const val USS = "USS"
    }
  }
}

fun JobsWsDialogState.initEmptyUuids(crudable: Crudable): JobsWsDialogState {
  return this.apply {
    uuid = crudable.nextUniqueValue<JobsWorkingSetConfig, String>()
  }
}

fun JobsWorkingSetConfig.toDialogState(): JobsWsDialogState {
  return JobsWsDialogState(
    uuid = this.uuid,
    connectionUuid = this.connectionConfigUuid,
    workingSetName = this.name,
    maskRow = this.jobsFilters.map { JobsWsDialogState.TableRow(it.prefix, it.owner, it.jobId) }
      .toMutableSmartList()
  )
}
