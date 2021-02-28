package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.explorer.ui.FileExplorerContent
import eu.ibagroup.formainframe.explorer.ui.SELECTED_NODES
import eu.ibagroup.formainframe.explorer.ui.cleanCacheIfPossible
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.utils.service

class DeleteAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES) ?: return
    val files = selected.filter {
      service<DataOpsManager>(it.node.explorer.componentManager).isOperationSupported(
        DeleteOperation(
          listOf(it.file ?: return@filter false)
        )
      )
    }.mapNotNull { it.file }
    if (files.isNotEmpty()) {
      val node = selected.getOrNull(0)?.node ?: return
      service<DataOpsManager>(node.explorer.componentManager).performOperation(
        operation = DeleteOperation(files),
        callback = fetchAdapter {
          onSuccess {
            node.parent?.cleanCacheIfPossible()
            sendTopic(FileExplorerContent.NODE_UPDATE, node.explorer.componentManager)(node.parent ?: return@onSuccess, true)
          }
        },
        project = e.project
      )
    }
  }

  override fun update(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES) ?: run {
      e.presentation.isVisible = false
      return
    }
    e.presentation.isVisible = selected.any {
      service<DataOpsManager>(it.node.explorer.componentManager).isOperationSupported(
        DeleteOperation(
          listOf(it.file ?: return@any false)
        )
      )
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

}