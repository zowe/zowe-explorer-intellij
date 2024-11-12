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
 */

package org.zowe.explorer.v3.operations

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.ConnectionConfig

/** Service to provide the endpoint which will run operations */
@Service(Service.Level.APP)
class OperationsService {

  companion object {
    private val EP = ExtensionPointName.create<OperationRunner<*, *, *>>("org.zowe.explorer.operationRunnerV3")
    fun getService(): OperationsService = service()
  }

  /**
   * Find the [OperationRunner] by the provided [OperationData] class
   * @param operation the class instance, inherited from [OperationData] class to search the respective operation runner by
   */
  private fun <R : Any, C : ConnectionConfig, O : OperationData<R, C>> findOperationRunner(
    operation: O
  ): OperationRunner<R, C, O>? {
    val foundRunner = EP.extensionList
      .find { it.operationDataClass == operation::class.java }
    if (foundRunner == null) {
      NotificationsService.errorNotification(
        NotificationCompatibleException("Operation runner for operation $operation is not found")
      )
    }
    @Suppress("UNCHECKED_CAST")
    return foundRunner as OperationRunner<R, C, O>?
  }

  // TODO: LoggerService
  /**
   * Perform operation for the provided operation
   * @param operationData the operation data instance to run the operation with
   * @param progressIndicator the progress indicator to finish the operation by
   */
  fun <R : Any, C : ConnectionConfig, O : OperationData<R, C>> performOperation(
    operationData: O,
    progressIndicator: ProgressIndicator
  ): R? {
    val operationRunner = findOperationRunner(operationData)
//    var startOpMessage = "Operation '${opRunner.operationClass.simpleName}' has been started"
//    if (operation is Query<*, *>) {
//      startOpMessage += "\nRequest params: ${operation.request}"
//    }
    val result = runCatching {
//      operationRunner.log.info(startOpMessage)
      val canRun = operationRunner?.canRun(operationData)
      if (canRun == true) {
        operationRunner.run(operationData, progressIndicator)
      } else {
        if (canRun != null) {
          NotificationsService.errorNotification(
            NotificationCompatibleException("The operation $operationData is not supported by the $operationRunner")
          )
        }
        null
      }
    }.onSuccess {
//      opRunner.log.info("Operation '${opRunner.operationClass.simpleName}' has been completed successfully")
    }.onFailure {
//      opRunner.log.info("Operation '${opRunner.operationClass.simpleName}' has failed", it)
      throw it
    }
    return result.getOrNull()
  }

}
