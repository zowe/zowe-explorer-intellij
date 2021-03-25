package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.explorer.ui.*

class AddMaskAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val selectedNode = e.getData(SELECTED_NODES)?.get(0)

    if (selectedNode != null) {
      val node = selectedNode.node
      if (node is WorkingSetNode) {
        val ws = selectedNode.node.value as WorkingSet
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
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES)
    val node = selected?.getOrNull(0)?.node
    e.presentation.isEnabledAndVisible = selected?.size == 1
        && node is WorkingSetNode
  }
}