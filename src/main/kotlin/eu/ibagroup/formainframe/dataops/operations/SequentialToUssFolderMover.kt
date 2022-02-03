package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.Requester
import eu.ibagroup.formainframe.dataops.attributes.USS_DELIMITER
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.getParentsChain
import eu.ibagroup.r2z.CopyDataUSS
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath
import retrofit2.Call
import java.io.IOException

class SequentialToUssFolderFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return SequentialToUssFolderMover(dataOpsManager)
  }
}

class SequentialToUssFolderMover(dataOpsManager: DataOpsManager) : DefaultFileMover(dataOpsManager) {

  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.destinationAttributes is RemoteUssAttributes
        && operation.destination.isDirectory
        && !operation.source.isDirectory
        && operation.sourceAttributes is RemoteDatasetAttributes
        && operation.commonUrls(dataOpsManager).isNotEmpty()
        && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  override fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester, ConnectionConfig>
  ): Call<Void> {
    val destinationAttributes = operation.destinationAttributes as RemoteUssAttributes
    val dataset = operation.sourceAttributes as RemoteDatasetAttributes
    val to = destinationAttributes.path + USS_DELIMITER + (operation.newName ?: dataset.name)
    return api<DataAPI>(
      url = requesterWithUrl.second.url,
      isAllowSelfSigned = requesterWithUrl.second.isAllowSelfSigned
    ).copyDatasetOrMemberToUss(
      authorizationToken = requesterWithUrl.first.connectionConfig.authToken,
      body = CopyDataUSS.CopyFromDataset(
        from = CopyDataUSS.CopyFromDataset.Dataset(dataset.name)
      ),
      filePath = FilePath(to)
    )
  }


}
