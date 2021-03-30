package eu.ibagroup.formainframe.dataops.operations

import com.google.gson.Gson
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.ErrorBodyAllocationException
import eu.ibagroup.r2z.*

class DatasetAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return DatasetAllocator(dataOpsManager)
  }
}

data class DatasetAllocationOperation(
  override val request: DatasetAllocationParams,
  override val connectionConfig: ConnectionConfig,
  override val urlConnection: UrlConnection
) : RemoteAllocationOperation<DatasetAllocationParams>

class DatasetAllocator(
  dataOpsManager: DataOpsManager
) : RemoteAllocatorBase<DatasetAllocationOperation>(dataOpsManager) {

  override fun performAllocationRequest(
    query: DatasetAllocationOperation,
    progressIndicator: ProgressIndicator?
  ) {
    progressIndicator?.checkCanceled()
    val response = api<DataAPI>(query.connectionConfig).createDataset(
      authorizationToken = query.connectionConfig.token,
      datasetName = query.request.datasetName,
      body = query.request.allocationParameters
    ).execute()
    if (!response.isSuccessful) {
      val gson = Gson()
      val errorParams= gson.fromJson(response.errorBody()?.charStream(),Map::class.java)
      throw ErrorBodyAllocationException("Cannot allocate dataset ${query.request.datasetName} on ${query.connectionConfig.name}", errorParams)
    }
  }

  override val operationClass = DatasetAllocationOperation::class.java

}

data class DatasetAllocationParams(
  var datasetName: String = "",
  val allocationParameters: CreateDataset = CreateDataset(
    allocationUnit = AllocationUnit.TRK,
    primaryAllocation = 0,
    secondaryAllocation = 0,
    recordFormat = RecordFormat.FB,
    datasetOrganization = DatasetOrganization.PS
  )
)