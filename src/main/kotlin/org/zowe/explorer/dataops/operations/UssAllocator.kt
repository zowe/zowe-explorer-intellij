/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import eu.ibagroup.r2z.CreateUssFile
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath

class UssAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return UssAllocator()
  }
}

data class UssAllocationParams(
  val parameters: CreateUssFile,
  val fileName: String,
  val path: String,
)

data class UssAllocationOperation(
  override val request: UssAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<UssAllocationParams>

class UssAllocator : Allocator<UssAllocationOperation> {

  override val operationClass = UssAllocationOperation::class.java

  override fun run(
    operation: UssAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).createUssFile(
      authorizationToken = operation.connectionConfig.authToken,
      filePath = FilePath(operation.request.path + "/" + operation.request.fileName),
      body = operation.request.parameters
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot allocate file ${operation.request.fileName} on ${operation.connectionConfig.name}"
      )
    }
  }
}
