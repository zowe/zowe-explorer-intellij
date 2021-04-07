package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.explorer.ui.*

class AddMaskAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return

    val ws = getUnits(view).firstOrNull() ?: return
    val initialState = MaskState(ws)
    val dialog = AddMaskDialog(e.project, initialState)
    if (dialog.showAndGet()) {
      val state = dialog.state
      when (state.type) {
        MaskState.ZOS -> ws.addMask(DSMask(state.mask, mutableListOf(), "", state.isSingle))
        MaskState.USS -> ws.addUssPath(UssPath(state.mask))
      }
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = getUnits(view).size == 1
  }

  private fun getUnits(view: GlobalFileExplorerView): List<WorkingSet> {
    return view.mySelectedNodesData.map { it.node }
      .filterIsInstance<ExplorerUnitTreeNodeBase<*, WorkingSet>>()
      .map { it.unit }
      .distinct()
  }

}