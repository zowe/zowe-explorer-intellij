package eu.ibagroup.formainframe.explorer.actions

import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.*
import eu.ibagroup.formainframe.config.ws.ui.jobs.JobsWsDialog
import eu.ibagroup.formainframe.utils.crudable.Crudable

class AddJobsWorkingSetAction : AddWsActionBase() {
  override val presentationTextInExplorer = ""
  override val defaultPresentationText = ""

  override fun createDialog(configCrudable: Crudable): AbstractWsDialog<*, *, out AbstractWsDialogState<out WorkingSetConfig, *>> {
    return JobsWsDialog(configCrudable, JobsWorkingSetDialogState().initEmptyUuids(configCrudable))
  }
}
