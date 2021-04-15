package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW

class ShowWsInfo : ToggleAction() {

  override fun isSelected(e: AnActionEvent): Boolean {
    return e.getData(FILE_EXPLORER_VIEW)?.myFsTreeStructure?.showWorkingSetInfo == true
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    view.myFsTreeStructure.showWorkingSetInfo = state
    view.myFsTreeStructure.findByValue(view.explorer).forEach {
      view.myStructure.invalidate(it, true)
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.getData(FILE_EXPLORER_VIEW) != null
  }

}