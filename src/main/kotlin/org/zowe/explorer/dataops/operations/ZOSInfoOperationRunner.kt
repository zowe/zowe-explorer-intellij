package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.kotlinsdk.InfoAPI
import org.zowe.kotlinsdk.InfoResponse

/**
 * Factory class to build an instance of system info operation runner. Defined in plugin.xml
 */
class ZOSInfoOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return ZOSInfoOperationRunner()
  }
}

/**
 * Base class implementation for running system info operation.
 */
class ZOSInfoOperationRunner : OperationRunner<ZOSInfoOperation, InfoResponse> {
  override val operationClass = ZOSInfoOperation::class.java
  override val resultClass = InfoResponse::class.java

  /**
   * Method determines if an operation can be run
   * @param operation - represents an operation
   */
  override fun canRun(operation: ZOSInfoOperation) = true

  /**
   * Method for running system info operation and return response body to caller
   * @param operation - represents an operation to be run
   * @param progressIndicator - represents a progress indicator object
   * @throws CallException if any error occurred or response body is null
   * @return InfoResponse serialized object
   */
  override fun run(operation: ZOSInfoOperation, progressIndicator: ProgressIndicator): InfoResponse {
    val response = api<InfoAPI>(connectionConfig = operation.connectionConfig)
      .getSystemInfo()
      .cancelByIndicator(progressIndicator)
      .execute()
    if (!response.isSuccessful) {
      throw CallException(response, "An internal error has occurred")
    }
    return response.body() ?: throw CallException(response, "Cannot parse z/OSMF info request body")
  }
}
