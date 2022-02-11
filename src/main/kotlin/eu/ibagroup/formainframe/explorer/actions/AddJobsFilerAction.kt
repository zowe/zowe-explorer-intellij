package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.*

class AddJobsFilerAction: AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val owner = ws.connectionConfig?.let { CredentialService.instance.getUsernameByKey(it.uuid ) } ?: ""
    val initialState = JobsFilterState(ws, "*", owner)
    val dialog = AddJobsFilterDialog(e.project, initialState)
    if (dialog.showAndGet()) {
      ws.addMask(dialog.state.toJobsFilter())
    }
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(JES_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getUnits(view).size == 1
  }

  private fun getUnits(view: JesExplorerView): List<JesWorkingSet> {
    return view.mySelectedNodesData.map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<*, JesWorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}
