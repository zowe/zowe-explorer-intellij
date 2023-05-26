/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkNoDialog
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.analytics.events.FileType
import eu.ibagroup.formainframe.common.ui.cleanInvalidateOnExpand
import eu.ibagroup.formainframe.common.ui.showUntilDone
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationOperation
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.r2z.*

/**
 * Action class for dataset allocation with parameters chosen by user
 */
class AllocateDatasetAction : AnAction() {

  /**
   * Called when allocate option is chosen from context menu,
   * runs allocate dataset operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    doAllocateAction(e)
  }

  /**
   * Determines if dataset allocation is possible for chosen object
   */
  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    e.presentation.isEnabledAndVisible = selected.getOrNull(0)?.node is MFNode
    e.presentation.icon = IconUtil.addText(AllIcons.FileTypes.Any_type, "DS")
  }

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}

/**
 * Allocates dataset
 * @param e action event object which contains links to entities with which should operation be performed
 * @param initialState contains state/parameters with which dataset should be allocated
 */
private fun doAllocateAction(e: AnActionEvent, initialState: DatasetAllocationParams = DatasetAllocationParams()) {
  val view = e.getData(FILE_EXPLORER_VIEW) ?: return
  val parentNode = view.mySelectedNodesData[0].node
  if (parentNode is ExplorerUnitTreeNodeBase<*, *> && parentNode.unit is FilesWorkingSet) {
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
          }
            .onSuccess {
              res = true
              var p: ExplorerTreeNode<*>? = parentNode
              while (p !is DSMaskNode) {
                p = p?.parent ?: break
              }
              val nodeToClean = p?.castOrNull<FileFetchNode<*,*,*,*,*>>()
              nodeToClean?.let { cleanInvalidateOnExpand(nodeToClean, view) }

              var nodeCleaned = false
              runInEdt {
                if (
                  showOkNoDialog(
                    title = "Dataset ${state.datasetName} Has Been Created",
                    message = "Would you like to add mask \"${state.datasetName}\" to ${parentNode.unit.name}",
                    project = e.project,
                    okText = "Yes",
                    noText = "No"
                  )
                ) {
                  val filesWorkingSetConfig =
                    configCrudable.getByUniqueKey<FilesWorkingSetConfig>(workingSet.uuid)?.clone()
                  if (filesWorkingSetConfig != null) {
                    nodeToClean?.cleanCache(recursively = false, cleanBatchedQuery = true, sendTopic = false)
                    nodeCleaned = true

                    filesWorkingSetConfig.dsMasks.add(DSMask().apply { mask = state.datasetName })
                    configCrudable.update(filesWorkingSetConfig)
                  }
                }

                if (!nodeCleaned) {
                  nodeToClean?.cleanCache(recursively = false, cleanBatchedQuery = true)
                }
              }
              initialState.errorMessage = ""
            }
            .onFailure { t ->
              parentNode.explorer.reportThrowable(t, e.project)
              initialState.errorMessage = t.message ?: t.toString()
            }
        }
        res
      }

    }
  }
}

/**
 * Returns null if object doesn't contain anything
 * needed for allocation algorithm
 */
private fun Int?.toNullIfZero(): Int? {
  return if (this == 0) null else this
}

/**
 * Returns null if objects is empty
 * needed for allocation algorithm
 */
private fun String.toNullIfEmpty(): String? {
  return if (this.isBlank()) null else this
}

/**
 * Processes parameters which were set for dataset allocation
 * set some parameters to needed values depending on specific fields states
 * @param state contains parameters that were set by the user or by default
 * @return processed params of dataset for allocation
 */
private fun postProcessState(state: DatasetAllocationParams): DatasetAllocationParams {
  if (state.allocationParameters.datasetOrganization == DatasetOrganization.PS) {
    state.allocationParameters.directoryBlocks = null
  }
  if (state.allocationParameters.datasetOrganization == DatasetOrganization.POE) {
    state.allocationParameters.datasetOrganization = DatasetOrganization.PO
    state.allocationParameters.dsnType = DsnameType.LIBRARY
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

/**
 * Action class for dataset allocation based on existing dataset
 */
class AllocateLikeAction : AnAction() {

  /**
   * Called when allocate like option is chosen from context menu,
   * runs allocate dataset like operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val datasetInfo = (selected[0].attributes as RemoteDatasetAttributes).datasetInfo
    val initialState = DatasetAllocationParams().apply {
      allocationParameters.datasetOrganization = datasetInfo.datasetOrganization ?: DatasetOrganization.PS
      allocationParameters.allocationUnit = spaceUnitsToAllocationUnits(datasetInfo.spaceUnits) ?: AllocationUnit.TRK
      allocationParameters.blockSize = datasetInfo.blockSize
      allocationParameters.recordLength = datasetInfo.recordLength
      allocationParameters.recordFormat = datasetInfo.recordFormat ?: RecordFormat.FB
      allocationParameters.volumeSerial = datasetInfo.volumeSerial
      allocationParameters.deviceType = datasetInfo.deviceType
      allocationParameters.dsnType = datasetInfo.dsnameType
      allocationParameters.primaryAllocation = datasetInfo.sizeInTracks ?: 100
      allocationParameters.secondaryAllocation = (datasetInfo.sizeInTracks ?: 100) / 2
      allocationParameters.directoryBlocks =
        if (datasetInfo.datasetOrganization == DatasetOrganization.PO || datasetInfo.datasetOrganization == DatasetOrganization.POE) {
          (datasetInfo.sizeInTracks ?: 100) / 3
        } else {
          0
        }
    }
    doAllocateAction(e, initialState)
  }

  /**
   * Transforms info about space units of existing datasets to format that is suitable for allocation operation
   * @param spaceUnits space units info of existing dataset
   * @return processed info about space units
   */
  private fun spaceUnitsToAllocationUnits(spaceUnits: SpaceUnits?): AllocationUnit? {
    if (spaceUnits == SpaceUnits.TRACKS) {
      return AllocationUnit.TRK
    }
    if (spaceUnits == SpaceUnits.CYLINDERS) {
      return AllocationUnit.CYL
    }
    if (spaceUnits == SpaceUnits.BLOCKS) {
      Messages.showWarningDialog(
        "Allocation unit BLK is not supported. It will be changed to TRK.",
        "Allocation Unit Will Be Changed"
      )
    }
    return null
  }

  /**
   * Determines if dataset allocation is possible for chosen object
   */
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

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
