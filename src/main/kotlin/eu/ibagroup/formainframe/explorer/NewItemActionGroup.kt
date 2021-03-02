package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import eu.ibagroup.formainframe.explorer.ui.*

class NewItemActionGroup : DefaultActionGroup() {


  override fun update(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES)
    val node = selected?.getOrNull(0)?.node
    e.presentation.isEnabledAndVisible = selected?.size == 1
      && (node is WorkingSetNode
      || node is DSMaskNode
      || node is UssDirNode
      || node is LibraryNode)
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}