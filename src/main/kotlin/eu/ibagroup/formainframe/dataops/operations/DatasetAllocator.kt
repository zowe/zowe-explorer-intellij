/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.*

class DatasetAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return DatasetAllocator()
  }
}

data class DatasetAllocationOperation(
  override val request: DatasetAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<DatasetAllocationParams>

class DatasetAllocator : Allocator<DatasetAllocationOperation> {

  override fun run(
    operation: DatasetAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).createDataset(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.datasetName,
      body = operation.request.allocationParameters
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(response, "Cannot allocate dataset ${operation.request.datasetName} on ${operation.connectionConfig.name}")
    }
  }

  override val operationClass = DatasetAllocationOperation::class.java

}

data class DatasetAllocationParams(
  var datasetName: String = "",
  var errorMessage: String = "",
  val allocationParameters: CreateDataset = CreateDataset(
    allocationUnit = AllocationUnit.TRK,
    primaryAllocation = 0,
    secondaryAllocation = 0,
    recordFormat = RecordFormat.FB,
    datasetOrganization = DatasetOrganization.PS
  )
)