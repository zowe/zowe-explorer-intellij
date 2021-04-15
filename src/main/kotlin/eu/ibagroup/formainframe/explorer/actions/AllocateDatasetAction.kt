package eu.ibagroup.formainframe.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkNoDialog
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationOperation
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.RecordFormat

class AllocateDatasetAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: return
    val parentNode = view.mySelectedNodesData[0].node
    if (parentNode is ExplorerUnitTreeNodeBase<*, *> && parentNode.unit is WorkingSet) {
      val workingSet = parentNode.unit
      val config = parentNode.unit.connectionConfig
      val urlConfig = parentNode.unit.urlConnection
      if (config != null && urlConfig != null) {
        showUntilDone(DatasetAllocationParams(), { initState ->
          AllocationDialog(project = e.project, initState)
        }) {
          val state = postProcessState(it)
          var res = false
          runModalTask(
            title = "Allocating Data Set ${state.datasetName}",
            project = e.project,
            cancellable = true
          ) {
            runCatching {
              parentNode.unit.explorer.componentManager.service<DataOpsManager>()
                .performOperation(
                  operation = DatasetAllocationOperation(
                    request = state,
                    connectionConfig = config,
                    urlConnection = urlConfig
                  ),
                  it
                )
            }.onSuccess {
              res = true
              var p: ExplorerTreeNode<*>? = parentNode
              while (p !is DSMaskNode) {
                p = p?.parent ?: break
              }
              p?.cleanCacheIfPossible()
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
            }.onFailure { t ->
              parentNode.explorer.reportThrowable(t, e.project)
            }
          }
          res
        }

      }
    }
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.getOrNull(0)?.node is MFNode
    e.presentation.icon = IconUtil.addText(AllIcons.FileTypes.Any_type, "DS")
  }

  private fun postProcessState(state: DatasetAllocationParams): DatasetAllocationParams {
    if (state.allocationParameters.datasetOrganization != DatasetOrganization.PO) {
      state.allocationParameters.directoryBlocks = null
    } else if (state.allocationParameters.recordFormat != RecordFormat.FB || state.allocationParameters.recordFormat != RecordFormat.VB) {
      state.allocationParameters.blockSize = null
    }
    return state.clone()
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}