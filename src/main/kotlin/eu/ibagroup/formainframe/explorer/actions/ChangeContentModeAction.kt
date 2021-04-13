package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.GlobalFileExplorerView
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.r2z.XIBMDataType

class ChangeContentModeAction : ToggleAction() {

  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return false
    return getMappedNodes(view)
      .mapNotNull {
        view.explorer.componentManager.service<DataOpsManager>()
          .getAttributesService(it.first::class.java, it.second::class.java)
          .getAttributes(it.second)
      }
      .all { it.contentMode == XIBMDataType.BINARY }
  }

  private fun getMappedNodes(view: GlobalFileExplorerView): List<Pair<VFileInfoAttributes, VirtualFile>> {
    return view.mySelectedNodesData
      .mapNotNull {
        if (it.file?.isDirectory == true) {
          return@mapNotNull null
        }
        Pair(it.attributes ?: return@mapNotNull null, it.file ?: return@mapNotNull null)
      }
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    getMappedNodes(view)
      .forEach {
        view.explorer.componentManager.service<DataOpsManager>()
          .getAttributesService(it.first::class.java, it.second::class.java)
          .updateAttributes(it.first) {
            contentMode = if (state) {
              XIBMDataType.BINARY
            } else {
              XIBMDataType.TEXT
            }
          }
      }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = !getMappedNodes(view).isNullOrEmpty()
  }


  override fun isDumbAware(): Boolean {
    return true
  }
}