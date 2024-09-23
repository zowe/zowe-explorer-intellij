/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import org.zowe.kotlinsdk.AllocationUnit
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.RecordFormat
import org.zowe.kotlinsdk.SpaceUnits

/**
 * Class that represents the "Allocate like" action
 */
class AllocateLikeAction : AllocateActionBase() {

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
   * Creates the initial dialog state with dataset attributes that are the same as the selected one's
   * @param datasetInfo selected dataset info to get predefined attributes for a new dataset
   * @return [DatasetAllocationParams] attributes of a new dataset that are the same as in the selected one
   */
  override fun preProcessState(datasetInfo: Dataset): DatasetAllocationParams {
    return DatasetAllocationParams().apply {
      allocationParameters.datasetOrganization = datasetInfo.datasetOrganization ?: DatasetOrganization.PS
      allocationParameters.allocationUnit = spaceUnitsToAllocationUnits(datasetInfo.spaceUnits) ?: AllocationUnit.TRK
      allocationParameters.blockSize = datasetInfo.blockSize
      allocationParameters.recordLength = datasetInfo.recordLength
      allocationParameters.recordFormat = datasetInfo.recordFormat ?: RecordFormat.FB
      allocationParameters.volumeSerial = datasetInfo.volumeSerial
      allocationParameters.deviceType = datasetInfo.deviceType
      allocationParameters.dsnType = datasetInfo.dsnameType
      allocationParameters.primaryAllocation =
        if (datasetInfo.spaceUnits == SpaceUnits.CYLINDERS) {
          (datasetInfo.sizeInTracks ?: 15) / 15
        } else {
          datasetInfo.sizeInTracks ?: 100
        }
      allocationParameters.secondaryAllocation = allocationParameters.primaryAllocation / 2
      allocationParameters.directoryBlocks =
        if (datasetInfo.datasetOrganization == DatasetOrganization.PO || datasetInfo.datasetOrganization == DatasetOrganization.POE) {
          allocationParameters.primaryAllocation / 3
        } else {
          0
        }
    }
  }

  /**
   * Called when allocate like option is chosen from context menu,
   * runs "allocate dataset like" operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      return
    }
    val selectedNodesData = view.mySelectedNodesData
    val datasetInfo = (selectedNodesData[0].attributes as RemoteDatasetAttributes).datasetInfo
    doAllocateAction(e, datasetInfo)
  }

  /**
   * Determines if dataset allocation is possible for chosen object
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val entityAttributes = if (selected.size == 1) selected[0].attributes else null
    e.presentation.isEnabledAndVisible = entityAttributes != null
      && entityAttributes is RemoteDatasetAttributes
      && entityAttributes.hasDsOrg
    e.presentation.icon = IconUtil.addText(AllIcons.FileTypes.Any_type, "DS")
  }

}
