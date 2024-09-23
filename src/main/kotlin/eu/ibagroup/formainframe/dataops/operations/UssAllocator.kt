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

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import org.zowe.kotlinsdk.CreateUssFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath

/**
 * Class which represents factory for uss allocator operation runner. Defined in plugin.xml
 */
class UssAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return UssAllocator()
  }
}

/**
 * Data class which represents input parameters for uss allocation operation
 * @param parameters instance of CreateUssFile object
 * @param fileName name of allocated file
 * @param path path of allocated file
 */
data class UssAllocationParams(
  val parameters: CreateUssFile,
  val fileName: String,
  val path: String,
)

/**
 * Data class which represents uss allocation operation object
 */
data class UssAllocationOperation(
  override val request: UssAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<UssAllocationParams>

/**
 * Class which represents uss allocator operation runner
 */
class UssAllocator : Allocator<UssAllocationOperation> {

  override val operationClass = UssAllocationOperation::class.java

  override val log = log<UssAllocator>()

  /**
   * Runs an uss allocation operation
   * @param operation uss allocation operation to be run
   * @param progressIndicator progress indicator object
   * @throws CallException if request is nor successful
   * @return Void
   */
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
