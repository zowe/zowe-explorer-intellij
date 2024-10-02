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

package org.zowe.explorer.explorer.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runModalTask
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CredentialsNotFoundForConnectionException
import org.zowe.explorer.dataops.operations.DatasetAllocationOperation
import org.zowe.explorer.dataops.operations.DatasetAllocationParams
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.DsnameType

const val ALLOCATE_ACTION_NOTIFICATION_GROUP_ID = "org.zowe.explorer.explorer.AllocateActionNotificationGroup"

abstract class AllocateActionBase : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
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
    return this.ifBlank { null }
  }

  /**
   * Creates the initial dialog state with empty dataset parameters
   * @param datasetInfo selected dataset info for further manipulations with its parameters to create initial
   * dialog state (is not used for a completely new dataset allocation)
   * @return [DatasetAllocationParams] default parameters for a new dataset
   */
  protected open fun preProcessState(datasetInfo: Dataset): DatasetAllocationParams {
    return DatasetAllocationParams()
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
   * Allocates a new dataset (completely new or the same as the selected one if "Allocate like" is selected)
   * @param e action event object which contains links to entities with which should operation be performed
   * @param datasetInfo the selected dataset info to be copied to a new one if the new dataset
   * is being allocated with the same parameters as the selected one ("Allocate like" is selected)
   */
  protected fun doAllocateAction(e: AnActionEvent, datasetInfo: Dataset = Dataset()) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val parentNode = view.mySelectedNodesData[0].node
    val project = e.project
    if (parentNode is ExplorerUnitTreeNodeBase<*, *, *> && parentNode.unit is FilesWorkingSet) {
      val workingSet = parentNode.unit
      val config = workingSet.connectionConfig
      if (config != null) {
        try {
          CredentialService.getUsername(config)
        } catch (e: CredentialsNotFoundForConnectionException) {
          NotificationsService
            .errorNotification(
              NotificationCompatibleException(
                "Operation is not available",
                "Allocation operation is not available: username is not found. Try fixing the connection first"
              ),
              project
            )
          return
        }
        val initialState = preProcessState(datasetInfo)
        showUntilDone(
          initialState,
          { initState -> AllocationDialog(project = project, config, initState) }
        ) {
          val state = postProcessState(it)
          var res = false
          runModalTask(
            title = "Allocating Data Set ${state.datasetName}",
            project = project,
            cancellable = true
          ) { progressIndicator ->
            runCatching {
              val dataOpsManager = DataOpsManager.getService()
              dataOpsManager.performOperation(
                operation = DatasetAllocationOperation(request = state, connectionConfig = config),
                progressIndicator
              )
            }
              .onSuccess {
                res = true
                var parentProbablyDSMaskNode: ExplorerTreeNode<*, *>? = parentNode
                while (parentProbablyDSMaskNode !is DSMaskNode) {
                  parentProbablyDSMaskNode = parentProbablyDSMaskNode?.parent ?: break
                }
                val nodeToClean = parentProbablyDSMaskNode?.castOrNull<FileFetchNode<*, *, *, *, *, *>>()
                nodeToClean?.let { cleanInvalidateOnExpand(nodeToClean, view) }
                nodeToClean?.cleanCache(recursively = false, cleanBatchedQuery = true)
                initialState.errorMessage = ""

                showNotification(state, workingSet)
              }
              .onFailure { t ->
                NotificationsService.errorNotification(t, project)
                initialState.errorMessage = t.message ?: t.toString()
              }
          }
          res
        }

      }
    }
  }

  /**
   * This method is needed for interface implementation
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Shows a notification about successful allocation and suggest adding a mask to the working set
   */
  private fun showNotification(
    state: DatasetAllocationParams,
    workingSet: ExplorerUnit<*>
  ) {
    var maskedDataset = state.datasetName
    val lastIndexOfDot = maskedDataset.lastIndexOf('.')
    if (lastIndexOfDot > 0) {
      maskedDataset = state.datasetName.substring(0, lastIndexOfDot)
    }
    maskedDataset += ".*"
    val filesWorkingSetConfig =
      ConfigService.getService().crudable
        .getByUniqueKey<FilesWorkingSetConfig>(workingSet.uuid)
        ?.clone()
    if (filesWorkingSetConfig?.dsMasks?.any { it.mask == maskedDataset } == false) {
      val notification = Notification(
        ALLOCATE_ACTION_NOTIFICATION_GROUP_ID,
        "Dataset ${state.datasetName} has been created",
        "Would you like to add mask \"$maskedDataset\" to see the created dataset?",
        NotificationType.INFORMATION
      )
      notification.addActions(
        setOf(
          NotificationAction.createSimpleExpiring("Add mask") {
            filesWorkingSetConfig.dsMasks.add(DSMask().apply { mask = maskedDataset })
            ConfigService.getService().crudable.update(filesWorkingSetConfig)
          },
          NotificationAction.createSimpleExpiring("Skip") { }
        )
      )
      notification.setSuggestionType(true)
      Notifications.Bus.notify(notification)
    }
  }
}
