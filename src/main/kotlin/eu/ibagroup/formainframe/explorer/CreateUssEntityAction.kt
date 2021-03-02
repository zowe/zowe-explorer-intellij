package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQueryImpl
import eu.ibagroup.formainframe.dataops.allocation.AllocationStatus
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

abstract class CreateUssEntityAction : AnAction() {

  abstract val fileType : CreateFileState
  abstract val ussFileType : String

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
          val state = dialog.state
          dataOpsManager.getAllocator(CreateFileState::class.java, RemoteQueryImpl::class.java)
            .allocate(RemoteQueryImpl(state, connectionConfig, urlConnection), fetchAdapter {
              onSuccess {
                if (it == AllocationStatus.SUCCESS) {
                  node.cleanCacheIfPossible()
                } else {
                  runInEdt {
                    Messages.showErrorDialog(
                      "Cannot create $ussFileType ${state.fileName}",
                      "Cannot Create File/Directory"
                    )
                  }
                }
              }
              onThrowable {
                runInEdt {
                  Messages.showErrorDialog(
                    "Cannot create $ussFileType ${state.fileName}",
                    "Cannot Create File/Directory"
                  )
                }
              }
            }, e.project)
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