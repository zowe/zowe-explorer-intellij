package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.dataops.operations.UssAllocationOperation
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.FileType

abstract class CreateUssEntityAction : AnAction() {

  abstract val fileType: CreateFileDialogState
  abstract val ussFileType: String

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val selected = view.mySelectedNodesData[0]
    val selectedNode = selected.node
    val node = if (selectedNode is UssFileNode) {
      selectedNode.parent?.takeIf { it is UssDirNode }
    } else {
      selectedNode.takeIf { it is UssDirNode }
    } ?: return
    val file = node.virtualFile
    if (node is ExplorerUnitTreeNodeBase<*, *>) {
      val connectionConfig = node.unit.connectionConfig
      val urlConnection = node.unit.urlConnection
      if (connectionConfig == null || urlConnection == null) return
      val dataOpsManager = node.unit.explorer.componentManager.service<DataOpsManager>()
      val filePath = if (file != null) {
        dataOpsManager.getAttributesService<RemoteUssAttributes, MFVirtualFile>()
          .getAttributes(file)?.path
      } else {
        (node as UssDirNode).value.path
      }
      if (filePath != null) {
        showUntilDone(
          initialState = fileType.apply { path = filePath },
          { initState -> CreateFileDialog(e.project, state = initState, filePath = filePath) }
        ) {
          var res = false
          val allocationParams = it.toAllocationParams()
          val fileType = if (allocationParams.parameters.type == FileType.FILE) {
            "File"
          } else {
            "Directory"
          }
          runModalTask(
            title = "Creating $fileType ${allocationParams.fileName}",
            project = e.project,
            cancellable = true
          ) {
            runCatching {
              dataOpsManager.performOperation(
                operation = UssAllocationOperation(
                  request = allocationParams,
                  connectionConfig = connectionConfig,
                  urlConnection = urlConnection
                ),
                progressIndicator = it
              )
            }.onSuccess {
              node.castOrNull<UssDirNode>()?.cleanCache(false)
              res = true
            }.onFailure { t ->
              view.explorer.reportThrowable(t, e.project)
            }
          }
          res
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
    e.presentation.isEnabledAndVisible = selected.size == 1 && (selected[0].node is UssDirNode || selected[0].node is UssFileNode)
  }
}