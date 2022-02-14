/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.showOkNoDialog
import com.intellij.util.IconUtil
import org.zowe.explorer.analytics.AnalyticsService
import org.zowe.explorer.analytics.events.FileAction
import org.zowe.explorer.analytics.events.FileEvent
import org.zowe.explorer.analytics.events.FileType
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.operations.DatasetAllocationOperation
import org.zowe.explorer.dataops.operations.DatasetAllocationParams
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.service
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
                val filesWorkingSetConfig = configCrudable.getByUniqueKey<FilesWorkingSetConfig>(workingSet.uuid)?.clone()
                if (filesWorkingSetConfig != null) {
                  filesWorkingSetConfig.dsMasks.add(DSMask().apply { mask = state.datasetName })
                  configCrudable.update(filesWorkingSetConfig)
                }
              }
            }
            initialState.errorMessage = ""
          }.onFailure { t ->
            parentNode.explorer.reportThrowable(t, e.project)
            initialState.errorMessage = t.message ?: t.toString()
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
