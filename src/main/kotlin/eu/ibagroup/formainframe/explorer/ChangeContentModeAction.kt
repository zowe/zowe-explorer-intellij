package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.explorer.ui.FileLikeDatasetNode
import eu.ibagroup.formainframe.explorer.ui.SELECTED_NODES
import eu.ibagroup.formainframe.explorer.ui.UssFileNode
import eu.ibagroup.r2z.XIBMDataType

class ChangeContentModeAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedNodes = e.getData(SELECTED_NODES)
    val selectedNode = selectedNodes?.getOrNull(0)?.node
    val attributes = if (selectedNode is FileLikeDatasetNode) {
      DataOpsManager.instance.tryToGetAttributes(selectedNode.virtualFile)
    } else {
      DataOpsManager.instance.tryToGetAttributes((selectedNode as UssFileNode).virtualFile)
    }
    if (attributes != null) {
      val virtualFile = selectedNode.virtualFile
      if (virtualFile != null) {
        attributes.contentMode = if (attributes.contentMode == XIBMDataType.BINARY) {
          XIBMDataType.TEXT
        } else {
          XIBMDataType.BINARY
        }
        val service = DataOpsManager.instance.getAttributesService(attributes::class.java, virtualFile::class.java )
        service.updateAttributes(virtualFile, attributes)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val selectedNodes = e.getData(SELECTED_NODES)
    val selectedNode = selectedNodes?.getOrNull(0)?.node
    if (selectedNode is FileLikeDatasetNode || selectedNode is UssFileNode) {
      val attributes = DataOpsManager.instance.tryToGetAttributes(selectedNode.virtualFile!!)
      if (attributes != null) {
        e.presentation.text = if (attributes.contentMode == XIBMDataType.BINARY) {
          "Set Text Mode"
        } else {
          "Set Binary Mode"
        }
        e.presentation.isEnabledAndVisible = true
      }
    } else {
      e.presentation.isEnabledAndVisible = false
    }
  }


  override fun isDumbAware(): Boolean {
    return true
  }
}