package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.explorer.ui.*

class CreateUssFileAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val dialog = CreateFileDialog(e.project, emptyFileState)
    if (dialog.showAndGet()) {
      //Сomment
    }
  }

  override fun update(e: AnActionEvent) {
    val node = e.getData(CURRENT_NODE)
    e.presentation.isVisible = node is UssDirNode
  }
}