package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.dataops.*
import eu.ibagroup.formainframe.dataops.allocation.AllocationStatus
import eu.ibagroup.formainframe.dataops.allocation.MemberAllocationParams
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class AddMemberAction : AnAction() {


  override fun actionPerformed(e: AnActionEvent) {
    val currentNode = e.getData(SELECTED_NODES)?.get(0)?.node
    if (currentNode is ExplorerUnitTreeNodeBase<*, *>
      && currentNode.unit is WorkingSet
      && currentNode is FileCacheNode<*, *, *, *, *>
    ) {
      val connectionConfig = currentNode.unit.connectionConfig
      val connectionUrl = currentNode.unit.urlConnection
      val dataOpsManager = service<DataOpsManager>(currentNode.explorer.componentManager)
      if (currentNode is LibraryNode && connectionConfig != null && connectionUrl != null) {
        val parentName = dataOpsManager
          .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
          .getAttributes(currentNode.virtualFile)
          ?.name
        if (parentName != null) {
          val dialog = AddMemberDialog(e.project, MemberAllocationParams(datasetName = parentName))
          if (dialog.showAndGet()) {
            val state = dialog.state
            dataOpsManager.getAllocator(
              requestClass = MemberAllocationParams::class.java,
              queryClass = RemoteQuery::class.java
            ).allocate(
              query = RemoteQueryImpl(
                connectionConfig = connectionConfig,
                urlConnection = connectionUrl,
                request = state
              ),
              callback = fetchAdapter {
                onSuccess {
                  if (it == AllocationStatus.SUCCESS) {
                    runInEdt {
                      currentNode.cleanCache()
                    }
                  } else {
                    runInEdt {
                      Messages.showErrorDialog(
                        "Cannot create member ${state.memberName} ${state.datasetName} on ${connectionConfig.name}",
                        "Cannot Allocate Dataset"
                      )
                    }
                  }
                }
                onThrowable {
                  runInEdt {
                    Messages.showErrorDialog(
                      "Cannot create member ${state.memberName} ${state.datasetName} on ${connectionConfig.name}",
                      "Cannot Allocate Dataset"
                    )
                  }
                }
              }
            )
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
    e.presentation.isEnabledAndVisible = selected?.getOrNull(0)?.node is LibraryNode
  }

}


