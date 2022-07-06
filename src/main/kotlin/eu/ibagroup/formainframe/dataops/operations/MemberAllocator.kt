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
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.DataAPI

// TODO: doc
class MemberAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return MemberAllocator()
  }
}

data class MemberAllocationOperation(
  override val request: MemberAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<MemberAllocationParams>

class MemberAllocator : Allocator<MemberAllocationOperation> {

  override val operationClass = MemberAllocationOperation::class.java

  override fun run(
    operation: MemberAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val request = apiWithBytesConverter<DataAPI>(operation.connectionConfig).writeToDatasetMember(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.datasetName,
      memberName = operation.request.memberName,
      content = byteArrayOf()
    ).cancelByIndicator(progressIndicator).execute()
    if (!request.isSuccessful) {
      throw Throwable(request.code().toString())
    }
  }
}

data class MemberAllocationParams(val datasetName: String, var memberName: String = "")