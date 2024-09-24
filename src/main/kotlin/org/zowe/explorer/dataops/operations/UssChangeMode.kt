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
import org.zowe.kotlinsdk.ChangeMode
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath

/**
 * Class which represents factory for uss change mode operation runner. Defined in plugin.xml
 */
class UssChangeModeFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<UssChangeModeOperation, Unit> {
    return UssChangeMode()
  }
}

/**
 * Data class which represents input parameters for uss change mode operation
 * @param parameters instance of [ChangeMode] object
 * @param path path of uss file
 */
data class UssChangeModeParams(
  val parameters: ChangeMode,
  val path: String,
)

/**
 * Data class which represents uss change mode operation object
 */
data class UssChangeModeOperation(
  override val request: UssChangeModeParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<UssChangeModeParams>

/**
 * Class which represents uss change mode operation runner
 */
class UssChangeMode : OperationRunner<UssChangeModeOperation, Unit> {

  override val operationClass = UssChangeModeOperation::class.java
  override val resultClass = Unit::class.java
  override val log = log<UssChangeMode>()

  /**
   * Runs an uss change mode operation
   * @param operation uss change mode operation to be run
   * @param progressIndicator progress indicator object
   * @throws CallException if request is not successful
   * @return Void
   */
  override fun run(
    operation: UssChangeModeOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val response = api<DataAPI>(operation.connectionConfig).changeFileMode(
      authorizationToken = operation.connectionConfig.authToken,
      filePath = FilePath(operation.request.path),
      body = operation.request.parameters
    ).cancelByIndicator(progressIndicator).execute()
    if (!response.isSuccessful) {
      throw CallException(
        response,
        "Cannot change file mode on ${operation.request.path}"
      )
    }
  }

  override fun canRun(operation: UssChangeModeOperation): Boolean {
    return true
  }
}
