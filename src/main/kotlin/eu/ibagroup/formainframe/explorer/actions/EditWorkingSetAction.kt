package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.files.WorkingSetDialog
import eu.ibagroup.formainframe.config.ws.ui.files.toDialogState
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.FilesWorkingSetNode
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey


class EditWorkingSetAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val node = view.mySelectedNodesData[0].node
    if (node is FilesWorkingSetNode) {
      var selected = configCrudable.getByUniqueKey<FilesWorkingSetConfig>(node.value.uuid)?.clone() as FilesWorkingSetConfig
      WorkingSetDialog(configCrudable, selected.toDialogState().apply { mode = DialogMode.UPDATE }).apply {
        if (showAndGet()) {
          selected = state.workingSetConfig
          configCrudable.update(selected)
        }
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
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.size == 1 && (selected[0].node is FilesWorkingSetNode)
  }
}
