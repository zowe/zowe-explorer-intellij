package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.*
import eu.ibagroup.formainframe.config.ws.ui.jobs.JobsWsDialog
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.JES_EXPLORER_VIEW
import eu.ibagroup.formainframe.utils.crudable.Crudable

class AddJobsWorkingSetAction : AddWsActionBase() {
  override val explorerView = JES_EXPLORER_VIEW
  override val presentationTextInExplorer = "Jobs Working Set"
  override val defaultPresentationText = "Create Jobs Working Set"

  override fun createDialog(configCrudable: Crudable): AbstractWsDialog<*, *, out AbstractWsDialogState<out WorkingSetConfig, *>> {
    return JobsWsDialog(configCrudable, JobsWorkingSetDialogState().initEmptyUuids(configCrudable))
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    if (e.getData(FILE_EXPLORER_VIEW) != null) {
      e.presentation.isEnabledAndVisible = false
    }
  }
}
