package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkNoDialog
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationOperation
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.RecordFormat

class AllocateDataset : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val parentNode = e.getData(SELECTED_NODES)?.get(0)?.node
    if (parentNode is ExplorerUnitTreeNodeBase<*, *> && parentNode.unit is WorkingSet) {
      val workingSet = parentNode.unit
      val config = parentNode.unit.connectionConfig
      val urlConfig = parentNode.unit.urlConnection
      if (config != null && urlConfig != null) {
        val dialog = AllocationDialog(e.project, DatasetAllocationParams())
        if (dialog.showAndGet()) {
          val state = postProcessState(dialog.state)
          runBackgroundableTask(
            title = "Allocating Data Set ${state.datasetName}",
            project = e.project,
            cancellable = true
          ) {
            runCatching {
              service<DataOpsManager>(parentNode.unit.explorer.componentManager)
                .performOperation(
                  operation = DatasetAllocationOperation(
                    request = state,
                    connectionConfig = config,
                    urlConnection = urlConfig
                  ),
                  it
                )
            }.onSuccess {
              parentNode.cleanCacheIfPossible()
              runInEdt {
                if (showOkNoDialog(
                    title = "Dataset ${state.datasetName} Has Been Created",
                    message = "Would you like to add mask \"${state.datasetName}\" to ${parentNode.unit.name}",
                    project = e.project,
                    okText = "Yes",
                    noText = "No"
                  )
                ) {
                  val workingSetConfig = configCrudable.getByUniqueKey<WorkingSetConfig>(workingSet.uuid)?.clone()
                  if (workingSetConfig != null) {
                    workingSetConfig.dsMasks.add(DSMask().apply { mask = state.datasetName })
                    configCrudable.update(workingSetConfig)
                  }
                }
              }
            }.onFailure {
              runInEdt {
                Messages.showErrorDialog(
                  "Cannot allocate dataset ${state.datasetName} on ${config.name}",
                  "Cannot Allocate Dataset"
                )
              }
            }
          }
        }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES)
    e.presentation.isEnabledAndVisible = selected != null
      && selected.size == 1
      && (selected[0].node is WorkingSetNode || selected[0].node is DSMaskNode)
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
