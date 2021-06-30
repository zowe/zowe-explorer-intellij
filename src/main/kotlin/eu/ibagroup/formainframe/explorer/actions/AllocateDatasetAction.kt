package eu.ibagroup.formainframe.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkNoDialog
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.analytics.events.FileType
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationOperation
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.r2z.*

class AllocateDatasetAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    doAllocateAction(e)
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


  override fun isDumbAware(): Boolean {
    return true
  }
}

private fun doAllocateAction(e: AnActionEvent, initialState: DatasetAllocationParams = DatasetAllocationParams()) {
  val view = e.getData(FILE_EXPLORER_VIEW) ?: return
  val parentNode = view.mySelectedNodesData[0].node
  if (parentNode is ExplorerUnitTreeNodeBase<*, *> && parentNode.unit is WorkingSet) {
    val workingSet = parentNode.unit
    val config = parentNode.unit.connectionConfig
    if (config != null) {
      showUntilDone(initialState, { initState ->
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
            service<AnalyticsService>().trackAnalyticsEvent(FileEvent(FileType.DATASET, FileAction.CREATE))
            parentNode.unit.explorer.componentManager.service<DataOpsManager>()
              .performOperation(
                operation = DatasetAllocationOperation(
                  request = state,
                  connectionConfig = config
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


private fun Int?.toNullIfZero(): Int? {
  return if (this == 0) null else this
}

private fun String.toNullIfEmpty(): String? {
  return if (this.isBlank()) null else this
}

private fun postProcessState(state: DatasetAllocationParams): DatasetAllocationParams {
  if (state.allocationParameters.datasetOrganization != DatasetOrganization.PO) {
    state.allocationParameters.directoryBlocks = null
  }
  if (state.allocationParameters.datasetOrganization == DatasetOrganization.POE) {
    state.allocationParameters.datasetOrganization = DatasetOrganization.PO
    state.allocationParameters.dsnType = DsnameType.LIBRARY
  }
  if (state.allocationParameters.allocationUnit == AllocationUnit.BLK) {
    state.allocationParameters.allocationUnit = null
  }
  state.allocationParameters.directoryBlocks = state.allocationParameters.directoryBlocks.toNullIfZero()
  state.allocationParameters.blockSize = state.allocationParameters.blockSize.toNullIfZero()
  state.allocationParameters.averageBlockLength = state.allocationParameters.averageBlockLength.toNullIfZero()
  state.allocationParameters.recordLength = state.allocationParameters.recordLength.toNullIfZero()
  state.allocationParameters.managementClass = state.allocationParameters.managementClass?.toNullIfEmpty()
  state.allocationParameters.storageClass = state.allocationParameters.storageClass?.toNullIfEmpty()
  state.allocationParameters.deviceType = state.allocationParameters.deviceType?.toNullIfEmpty()
  state.allocationParameters.dataClass = state.allocationParameters.dataClass?.toNullIfEmpty()
  state.allocationParameters.volumeSerial = state.allocationParameters.volumeSerial?.toNullIfEmpty()
  return state.clone()
}

class AllocateLikeAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val datasetInfo = (selected[0].attributes as RemoteDatasetAttributes).datasetInfo
    val initialState = DatasetAllocationParams().apply {
      allocationParameters.datasetOrganization = datasetInfo.datasetOrganization ?: DatasetOrganization.PS
      allocationParameters.blockSize = datasetInfo.blockSize
      allocationParameters.recordLength = datasetInfo.recordLength
      allocationParameters.recordFormat = datasetInfo.recordFormat ?: RecordFormat.FB
      allocationParameters.volumeSerial = datasetInfo.volumeSerial
      allocationParameters.deviceType = datasetInfo.deviceType
      allocationParameters.dsnType = datasetInfo.dsnameType
      allocationParameters.primaryAllocation = datasetInfo.sizeInTracks ?: 100
      allocationParameters.secondaryAllocation = (datasetInfo.sizeInTracks ?: 100) / 2
      allocationParameters.directoryBlocks = if (datasetInfo.datasetOrganization == DatasetOrganization.PO) {
        (datasetInfo.sizeInTracks ?: 100) / 3
      } else {
        0
      }
    }
    doAllocateAction(e, initialState)
  }

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.size == 1
        && selected[0].attributes is RemoteDatasetAttributes
        && !(selected[0].attributes as RemoteDatasetAttributes).isMigrated
    e.presentation.icon = IconUtil.addText(AllIcons.FileTypes.Any_type, "DS")
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}