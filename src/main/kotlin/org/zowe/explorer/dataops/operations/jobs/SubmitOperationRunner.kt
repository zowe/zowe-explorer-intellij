/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations.jobs

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.cancelByIndicator
import eu.ibagroup.r2z.JESApi
import eu.ibagroup.r2z.SubmitFileNameBody
import eu.ibagroup.r2z.SubmitJobRequest

class SubmitOperationRunner : OperationRunner<SubmitJobOperation, SubmitJobRequest> {

  override val operationClass = SubmitJobOperation::class.java

  override fun run(operation: SubmitJobOperation, progressIndicator: ProgressIndicator): SubmitJobRequest {
    progressIndicator.checkCanceled()

    val fileName = when (val attributes = service<DataOpsManager>().tryToGetAttributes(operation.request.file)) {
      is RemoteUssAttributes -> {
        attributes.path
      }
      is RemoteDatasetAttributes -> {
        "//'${attributes.datasetInfo.name}'"
      }
      else -> {
        val castedAttributes = attributes as RemoteMemberAttributes
        val parentFileAttributes = service<DataOpsManager>().tryToGetAttributes(castedAttributes.parentFile) as RemoteDatasetAttributes
        "//'${parentFileAttributes.datasetInfo.name}(${castedAttributes.info.name})'"
      }
    }

    val response = api<JESApi>(operation.connectionConfig).submitJobRequest(
      basicCredentials = operation.connectionConfig.authToken,
      body = SubmitFileNameBody(fileName)
    ).cancelByIndicator(progressIndicator).execute()
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot submit ${operation.request.file.name} on ${operation.connectionConfig.name}"
      )
    }
    return body
  }

  override val resultClass = SubmitJobRequest::class.java

  override fun canRun(operation: SubmitJobOperation): Boolean {
    return true
  }
}

class SubmitJobOperationFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return SubmitOperationRunner()
  }
}

data class SubmitOperationParams(
  val file: VirtualFile
)

data class SubmitJobOperation(
  override val request: SubmitOperationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteQuery<SubmitOperationParams, SubmitJobRequest> {
  override val resultClass = SubmitJobRequest::class.java
}
