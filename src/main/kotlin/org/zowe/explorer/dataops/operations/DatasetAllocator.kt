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

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.config.connect.getUsername
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.explorer.config.Presets
import org.zowe.explorer.explorer.config.getSampleJclMemberContent
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.kotlinsdk.*
import java.lang.Exception

/**
 * Class which represents factory for dataset allocator operation runner. Defined in plugin.xml
 */
class DatasetAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return DatasetAllocator()
  }
}

/**
 * Data class which represents dataset allocation operation object
 */
data class DatasetAllocationOperation(
  override val request: DatasetAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<DatasetAllocationParams>

/**
 * Class which represents dataset allocator operation runner
 */
class DatasetAllocator : Allocator<DatasetAllocationOperation> {

  /**
   * Runs a dataset allocation operation
   * @param operation - dataset allocation operation to be run
   * @param progressIndicator - progress indicator object
   * @throws CallException if request is nor successful
   * @return Void
   */
  override fun run(
    operation: DatasetAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val datasetResponse = api<DataAPI>(operation.connectionConfig).createDataset(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.datasetName,
      body = operation.request.allocationParameters
    ).cancelByIndicator(progressIndicator).execute()
    if (!datasetResponse.isSuccessful) {
      throw CallException(
        datasetResponse,
        "Cannot allocate dataset ${operation.request.datasetName} on ${operation.connectionConfig.name}"
      )
    } else {
      if (operation.request.presets != Presets.CUSTOM_DATASET
        && operation.request.presets != Presets.SEQUENTIAL_DATASET
        && operation.request.presets != Presets.PDS_DATASET
      ) {
        // Allocate member
        var throwable: Throwable? = null
        runCatching {
          val memberResponse = apiWithBytesConverter<DataAPI>(operation.connectionConfig).writeToDatasetMember(
            authorizationToken = operation.connectionConfig.authToken,
            datasetName = operation.request.datasetName,
            memberName = operation.request.memberName,
            content = if (operation.request.presets == Presets.PDS_WITH_EMPTY_MEMBER) byteArrayOf()
              else getSampleJclMemberContent(getUsername(operation.connectionConfig)).encodeToByteArray()
          ).cancelByIndicator(progressIndicator).execute()
          if (!memberResponse.isSuccessful) {
            throwable = CallException(
              memberResponse,
              "Cannot create sample member ${operation.request.memberName} in ${operation.request.datasetName} " +
                  "on ${operation.connectionConfig.name}"
            )
          }
        }.onFailure { if(throwable != null) throw Throwable(cause = throwable) else throw Exception("Error allocating a new sample member ${operation.request.memberName}") }
      }
    }
  }

  override val operationClass = DatasetAllocationOperation::class.java

  override val log = log<DatasetAllocator>()

}

/**
 * Data class which represents input parameters for dataset allocation operation
 * @param datasetName - dataset name
 * @param errorMessage - error message
 * @param allocationParameters - instance of CreateDataset object with allocation parameters
 */
data class DatasetAllocationParams(
  var presets: Presets = Presets.CUSTOM_DATASET,
  var datasetName: String = "",
  var memberName: String = "",
  var errorMessage: String = "",
  val allocationParameters: CreateDataset = CreateDataset(
    allocationUnit = AllocationUnit.TRK,
    primaryAllocation = 1,
    secondaryAllocation = 0,
    recordFormat = RecordFormat.FB,
    datasetOrganization = DatasetOrganization.PS,
    recordLength = 80
  )
)
