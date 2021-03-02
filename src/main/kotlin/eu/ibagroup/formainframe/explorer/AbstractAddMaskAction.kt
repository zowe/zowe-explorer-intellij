package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.explorer.ui.SELECTED_NODES

abstract class AbstractAddMaskAction: AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    TODO("Not yet implemented")
  }

  override fun update(e: AnActionEvent) {
    val nodes = e.getData(SELECTED_NODES)

  }

  override fun isDumbAware(): Boolean {
    return true
  }

}