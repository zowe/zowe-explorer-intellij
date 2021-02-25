package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.*
import eu.ibagroup.formainframe.dataops.allocation.AllocationStatus
import eu.ibagroup.formainframe.dataops.allocation.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.RecordFormat

class AllocateDataset : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val parentNode = e.getData(CURRENT_NODE)
    if (parentNode is ExplorerUnitTreeNodeBase<*, *> && parentNode.unit is WorkingSet) {
      val workingSet = parentNode.unit
      val config = parentNode.unit.connectionConfig
      val urlConfig = parentNode.unit.urlConnection
      if (config != null && urlConfig != null) {
        val dialog = AllocationDialog(e.project, DatasetAllocationParams())
        if (dialog.showAndGet()) {
          val state = postProcessState(dialog.state)
          service<DataOpsManager>(parentNode.unit.explorer.componentManager).getAllocator(
            requestClass = DatasetAllocationParams::class.java,
            queryClass = RemoteQuery::class.java
          ).allocate(
            query = RemoteQueryImpl(
              connectionConfig = config,
              urlConnection = urlConfig,
              request = state
            ),
            callback = fetchAdapter {
              onSuccess {
                if (it == AllocationStatus.SUCCESS) {
                  runInEdt {
                    val workingSetConfig = configCrudable.getByUniqueKey<WorkingSetConfig>(workingSet.uuid)?.clone()
                    if (workingSetConfig != null) {
                      workingSetConfig.dsMasks.add(DSMask().apply { mask = state.datasetName })
                      configCrudable.update(workingSetConfig)
                    }
                  }
                } else {
                  runInEdt {
                    Messages.showErrorDialog(
                      "Cannot allocate dataset ${state.datasetName} on ${config.name}",
                      "Cannot Allocate Dataset"
                    )
                  }
                }
              }
              onThrowable {
                runInEdt {
                  Messages.showErrorDialog(
                    "Cannot allocate dataset ${state.datasetName} on ${config.name}",
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

  override fun update(e: AnActionEvent) {
    val node = e.getData(CURRENT_NODE)
    e.presentation.isVisible = node is WorkingSetNode || node is DSMaskNode
  }

  private fun postProcessState(state: DatasetAllocationParams): DatasetAllocationParams {
    if (state.allocationParameters.datasetOrganization != DatasetOrganization.PO) {
      state.allocationParameters.directoryBlocks = null
    } else if (state.allocationParameters.recordFormat != RecordFormat.FB || state.allocationParameters.recordFormat != RecordFormat.VB) {
      state.allocationParameters.blockSize = null
    }
    return state
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
