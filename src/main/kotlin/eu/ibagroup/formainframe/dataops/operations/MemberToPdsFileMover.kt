package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.r2z.CopyDataZOS
import eu.ibagroup.r2z.DataAPI
import retrofit2.Call
import java.io.FileNotFoundException
import java.io.IOException

class MemberToPdsFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return MemberToPdsFileMover(dataOpsManager)
  }
}

class MemberToPdsFileMover(
  private val dataOpsManager: DataOpsManager
) : OperationRunner<MoveCopyOperation, Unit> {

  private fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester, UrlConnection>
  ): Call<Void> {
    val destinationAttributes = operation.destinationAttributes as RemoteDatasetAttributes
    var memberName: String
    val datasetName = (operation.sourceAttributes as RemoteMemberAttributes).run {
      memberName = name
      dataOpsManager.tryToGetAttributes(libraryFile)?.name
        ?: throw FileNotFoundException("Cannot find attributes for ${libraryFile.path}")
    }
    return api<DataAPI>(
      url = requesterWithUrl.second.url,
      isAllowSelfSigned = requesterWithUrl.second.isAllowSelfSigned
    ).copyToDatasetMember(
      authorizationToken = requesterWithUrl.first.connectionConfig.token,
      body = CopyDataZOS.CopyFromDataset(
        dataset = CopyDataZOS.CopyFromDataset.Dataset(
          datasetName = datasetName,
          memberName = memberName
        ),
        replace = operation.forceOverwriting
      ),
      toDatasetName = destinationAttributes.name,
      memberName = operation.newName ?: memberName
    )
  }

  override val operationClass = MoveCopyOperation::class.java

  override val resultClass = Unit::class.java

  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.destinationAttributes is RemoteDatasetAttributes
      && operation.destination.isDirectory
      && !operation.source.isDirectory
      && operation.sourceAttributes is RemoteMemberAttributes
      && operation.commonUrls(dataOpsManager).isNotEmpty()
  }

  override fun run(
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ) {
    var throwable: Throwable? = null
    operation.commonUrls(dataOpsManager).stream().map {
      progressIndicator.checkCanceled()
      runCatching {
        buildCall(operation, it).cancelByIndicator(progressIndicator).execute()
      }.mapCatching {
        if (!it.isSuccessful) {
          throw IOException(it.code().toString())
        } else {
          it
        }
      }.mapCatching {
        val sourceAttributes = operation.sourceAttributes
        if (operation.isMove && sourceAttributes != null) {
          dataOpsManager.performOperation(DeleteOperation(operation.source, sourceAttributes))
        } else {
          it
        }
      }.onSuccess {
        return@map true
      }.onFailure {
        throwable = it
      }
      return@map false
    }.filter { it }.findAnyNullable()
    throwable?.let { throw it }
  }
}