package eu.ibagroup.formainframe.dataops.operations.jobs

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.*
import retrofit2.Response

class HoldJobOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return HoldJobOperationRunner()
  }
}

class HoldJobOperationRunner : OperationRunner<HoldJobOperation, HoldJobRequest> {

  override val operationClass = HoldJobOperation::class.java

  override val resultClass = HoldJobRequest::class.java

  override fun canRun(operation: HoldJobOperation): Boolean {
    return true
  }

  override fun run(operation: HoldJobOperation, progressIndicator: ProgressIndicator): HoldJobRequest {
    progressIndicator.checkCanceled()

    val response : Response<HoldJobRequest> = when (operation.request) {
      is BasicHoldJobParams -> {
        api<JESApi>(operation.connectionConfig).holdJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobName = operation.request.jobName,
          jobId = operation.request.jobId,
          body = HoldJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      is CorrelatorHoldJobParams -> {
        api<JESApi>(operation.connectionConfig).holdJobRequest(
          basicCredentials = operation.connectionConfig.authToken,
          jobCorrelator = operation.request.correlator,
          body = HoldJobRequestBody()
        ).cancelByIndicator(progressIndicator).execute()
      }
      else -> throw Exception("Method with such parameters not found")
    }
    val body = response.body()
    if (!response.isSuccessful || body == null) {
      throw CallException(
        response,
        "Cannot hold job on ${operation.connectionConfig.name}"
      )
    }
    return body
  }
}

open class HoldJobOperationParams

class BasicHoldJobParams(val jobName: String, val jobId: String)  : HoldJobOperationParams()

class CorrelatorHoldJobParams(val correlator: String) : HoldJobOperationParams()

data class HoldJobOperation(
  override val request: HoldJobOperationParams,
  override val connectionConfig: ConnectionConfig
) : RemoteQuery<HoldJobOperationParams, HoldJobRequest> {
  override val resultClass = HoldJobRequest::class.java
}