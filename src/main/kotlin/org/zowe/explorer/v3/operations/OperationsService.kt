/*
 * Copyright (c) 2024 IBA Group.
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
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.operations

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig

/** Service to provide the endpoint which will run operations */
@Service(Service.Level.APP)
class OperationsService {

  companion object {
    private val EP_NAME = ExtensionPointName.create<OperationRunner<*, *, *>>("org.zowe.explorer.operationRunnerV3")
    fun getService(): OperationsService = service()
  }

  /**
   * Find the [OperationRunner] by the provided [OperationData]
   * @param operationData the [OperationData] compatible class to search for the respective operation runner by
   * @return found operation runner or null
   */
  private fun <R : Any, C : HttpConnectionConfig, O : OperationData<R, C>> findOperationRunner(
    operationData: O
  ): OperationRunner<R, C, O>? {
    val foundRunner = EP_NAME.extensionList
      .find { it.operationDataClass == operationData::class.java }
    @Suppress("UNCHECKED_CAST")
    return foundRunner as OperationRunner<R, C, O>?
  }

  // TODO: LoggerService
  /**
   * Perform operation for the provided operation
   * @param operationData the operation data instance to run the operation with
   * @param progressIndicator the progress indicator to finish the operation by
   * @return a [Result] with success data or failure with the exception happened during either the operation preparation
   *         or the operation run
   */
  fun <R : Any, C : HttpConnectionConfig, O : OperationData<R, C>> performOperation(
    operationData: O,
    progressIndicator: ProgressIndicator
  ): Result<R> {
    val result = runCatching {
      val operationRunner = findOperationRunner(operationData)
        ?: throw NotificationCompatibleException(
            "Error during operation run",
            "Operation runner for operation-compatible $operationData is not found"
          )
      val canRun = operationRunner.canRun(operationData)
      if (!canRun) {
        throw NotificationCompatibleException(
          "Error during operation run",
          "The $operationRunner cannot be run with the provided $operationData"
        )
      }

      //    var startOpMessage = "Operation '${opRunner.operationClass.simpleName}' has been started"
      //    if (operation is Query<*, *>) {
      //      startOpMessage += "\nRequest params: ${operation.request}"
      //    }
      //      operationRunner.log.info(startOpMessage)
      operationRunner.run(operationData, progressIndicator)
    }
      .onSuccess {
//        opRunner.log.info("Operation '${opRunner.operationClass.simpleName}' has been completed successfully")
      }
      .onFailure {
//        opRunner.log.info("Operation '${opRunner.operationClass.simpleName}' has failed", it)
        NotificationsService.errorNotification(it)
      }
    return result
  }

}
