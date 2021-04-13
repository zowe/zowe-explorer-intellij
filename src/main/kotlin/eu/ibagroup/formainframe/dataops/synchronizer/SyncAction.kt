package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager

class SyncAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
  }

  override fun update(e: AnActionEvent) {
    val file = getSupportedVirtualFile(e) ?: let {
      makeDisabled(e)
      return
    }
    val editor = getEditor(e) ?: let {
      makeDisabled(e)
      return
    }
    e.presentation.isEnabledAndVisible = !(editor.document.text.toByteArray() contentEquals file.contentsToByteArray())
  }

  private fun makeDisabled(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
  }

  private fun getDataOpsManager(e: AnActionEvent): DataOpsManager {
    return service()
  }

  private fun getEditor(e: AnActionEvent): Editor? {
    return e.getData(CommonDataKeys.EDITOR)
  }

  private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
    return e.getData(CommonDataKeys.VIRTUAL_FILE)
  }

  private fun getSupportedVirtualFile(e: AnActionEvent): VirtualFile? {
    return getVirtualFile(e)?.let {
      if (getDataOpsManager(e).isSyncSupported(it)) {
        it
      } else {
        null
      }
    }
  }

}