package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import eu.ibagroup.formainframe.explorer.ui.*

class NewItemActionGroup : DefaultActionGroup() {


  override fun update(e: AnActionEvent) {
    val node = e.getData(CURRENT_NODE)
    e.presentation.isVisible = node is WorkingSetNode || node is DSMaskNode || node is UssDirNode || node is LibraryNode
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}