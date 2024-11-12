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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.kotlinsdk.ChangeOwner
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath

/**
 * Class which represents factory for uss change owner operation runner. Defined in plugin.xml
 */
class UssChangeOwnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<UssChangeOwnerOperation, Unit> {
    return UssChangeOwner()
  }
}

/**
 * Data class which represents input parameters for uss change owner operation
 * @param parameters instance of [ChangeOwner] object
 * @param path path of uss file
 */
data class UssChangeOwnerParams(
  val parameters: ChangeOwner,
  val path: String,
)

/**
 * Data class which represents uss change owner operation object
 */
data class UssChangeOwnerOperation(
  override val request: UssChangeOwnerParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<UssChangeOwnerParams>

/**
 * Class which represents uss change owner operation runner
 */
class UssChangeOwner : OperationRunner<UssChangeOwnerOperation, Unit> {

  override val operationClass = UssChangeOwnerOperation::class.java
  override val resultClass = Unit::class.java
  override val log = log<UssChangeOwner>()

  /**
   * Runs an uss change owner operation
   * @param operation uss change owner operation to be run
   * @param progressIndicator progress indicator object
   * @throws CallException if request is not successful
   * @return Void
   */
  override fun run(
    operation: UssChangeOwnerOperation, progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).changeFileOwner(
      authorizationToken = operation.connectionConfig.authToken,
      filePath = FilePath(operation.request.path),
      body = operation.request.parameters
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response, "Cannot change file owner on ${operation.request.path}"
      )
    }
  }

  override fun canRun(operation: UssChangeOwnerOperation): Boolean {
    return true
  }
}
