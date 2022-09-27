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
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.DataAPI

/**
 * Class which represents factory for member allocator operation runner. Defined in plugin.xml
 */
class MemberAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return MemberAllocator()
  }
}

/**
 * Data class which represents member allocation operation object
 */
data class MemberAllocationOperation(
  override val request: MemberAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<MemberAllocationParams>

/**
 * Class which represents member allocator operation runner
 */
class MemberAllocator : Allocator<MemberAllocationOperation> {

  override val operationClass = MemberAllocationOperation::class.java

  /**
   * Runs a member allocation operation
   * @param operation member allocation operation to be run
   * @param progressIndicator progress indicator object
   * @throws Throwable if request is nor successful
   * @return Void
   */
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
      throw CallException(
        request,
        "Cannot create member ${operation.request.memberName} in ${operation.request.datasetName} " +
            "on ${operation.connectionConfig.name}"
      )
    }
  }
}

/**
 * Data class which represents input parameters for uss allocation operation
 */
data class MemberAllocationParams(val datasetName: String, var memberName: String = "")