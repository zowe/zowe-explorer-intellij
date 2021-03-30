package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.ErrorBodyAllocationException
import eu.ibagroup.formainframe.dataops.exceptions.getFullAllocationErrorString
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.dataops.operations.UssAllocationOperation
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.FileType

abstract class CreateUssEntityAction : AnAction() {

  abstract val fileType: CreateFileDialogState
  abstract val ussFileType: String

  override fun actionPerformed(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES)?.get(0)
    val node = selected?.node
    val file = selected?.file
    if (file != null && node is ExplorerUnitTreeNodeBase<*, *>) {
      val connectionConfig = node.unit.connectionConfig
      val urlConnection = node.unit.urlConnection
      if (connectionConfig == null || urlConnection == null) return
      val dataOpsManager = service<DataOpsManager>(node.unit.explorer.componentManager)
      val filePath = dataOpsManager.getAttributesService<RemoteUssAttributes, MFVirtualFile>()
        .getAttributes(file)?.path
      if (filePath != null) {
        val dialog = CreateFileDialog(e.project, fileType.apply { path = filePath }, filePath)
        if (dialog.showAndGet()) {
          val allocationParams = dialog.state.toAllocationParams()
          val fileType = if (allocationParams.parameters.type == FileType.FILE) {
            "File"
          } else {
            "Directory"
          }
          runBackgroundableTask(
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
              node.cleanCacheIfPossible()
            }.onFailure {
              runInEdt {
                Messages.showErrorDialog(
                  getFullAllocationErrorString(it as ErrorBodyAllocationException),
                  "Cannot Allocate $fileType"
                )
              }
            }
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
    e.presentation.isEnabledAndVisible = selected != null
      && selected.size == 1
      && selected[0].node is UssDirNode
  }
}