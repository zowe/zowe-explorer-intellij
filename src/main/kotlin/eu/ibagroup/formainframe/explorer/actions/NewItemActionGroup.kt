package eu.ibagroup.formainframe.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_CONTEXT_MENU

class NewItemActionGroup : DefaultActionGroup() {


  override fun update(e: AnActionEvent) {
    e.presentation.icon = if (e.place != FILE_EXPLORER_CONTEXT_MENU) {
      null
    } else {
      AllIcons.General.Add
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}