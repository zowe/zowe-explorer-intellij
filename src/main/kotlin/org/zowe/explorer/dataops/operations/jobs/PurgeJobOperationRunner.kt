package org.zowe.explorer.dataops.operations.jobs

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.dataops.operations.OperationRunnerFactory
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.kotlinsdk.CancelJobPurgeOutRequest
import org.zowe.kotlinsdk.JESApi
import retrofit2.Response

/** Factory for purge job operation runner */
class PurgeJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return PurgeJobOperationRunner()
  }
}

/** Purge operation runner */
class PurgeJobOperationRunner : OperationRunner<PurgeJobOperation, CancelJobPurgeOutRequest> {

  override val operationClass = PurgeJobOperation::class.java

  override val resultClass = CancelJobPurgeOutRequest::class.java

  override fun canRun(operation: PurgeJobOperation): Boolean {
    return true
  }

  /**
   * Method that sends purge request to mf
   * @param operation describes the parameters to be sent and the connection configuration
   * @param progressIndicator to interrupt if the computation is canceled
   * @return [CancelJobPurgeOutRequest] body
   */
  override fun run(operation: PurgeJobOperation, progressIndicator: ProgressIndicator): CancelJobPurgeOutRequest {
    progressIndicator.checkCanceled()

    val response: Response<CancelJobPurgeOutRequest> = when (operation.request) {
      is BasicPurgeJobParams -> {
        api<JESApi>(operation.connectionConfig).cancelJobPurgeOutRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobName = operation.request.jobName,
          jobId = operation.request.jobId
        ).cancelByIndicator(progressIndicator).execute()
      }
      is CorrelatorPurgeJobParams -> {
        api<JESApi>(operation.connectionConfig).cancelJobPurgeOutRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobCorrelator = operation.request.correlator
        ).cancelByIndicator(progressIndicator).execute()
      }
      else -> throw Exception("Method with such parameters not found")
    }
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot purge job on ${operation.connectionConfig.name}"
      )
    }
    return body
  }
}

open class PurgeJobOperationParams

/** Job Name and Job Id are used */
class BasicPurgeJobParams(val jobName: String, val jobId: String) : PurgeJobOperationParams()

/** Correlator is used */
class CorrelatorPurgeJobParams(val correlator: String) : PurgeJobOperationParams()

/** Class for purge job operation */
data class PurgeJobOperation(
  override val request: PurgeJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<PurgeJobOperationParams, CancelJobPurgeOutRequest> {
  override val resultClass = CancelJobPurgeOutRequest::class.java
}
