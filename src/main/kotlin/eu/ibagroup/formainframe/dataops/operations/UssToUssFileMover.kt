package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.USS_DELIMITER
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.getParentsChain
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.r2z.CopyDataUSS
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath
import eu.ibagroup.r2z.MoveUssFile
import retrofit2.Call

class UssToUssFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return UssToUssFileMover(dataOpsManager)
  }
}

class UssToUssFileMover(private val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.sourceAttributes is RemoteUssAttributes
      && operation.destinationAttributes is RemoteUssAttributes
      && operation.destinationAttributes.isDirectory
      && operation.commonUrls(dataOpsManager).isNotEmpty()
      && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  private fun makeCall(
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Triple<Call<Void>, String, String> {
    val sourceAttributes = (operation.sourceAttributes as RemoteUssAttributes)
    val destinationAttributes = (operation.destinationAttributes as RemoteUssAttributes)
    val from = sourceAttributes.path
    val to = destinationAttributes.path + USS_DELIMITER + (operation.newName ?: sourceAttributes.name)
    val api = api<DataAPI>(connectionConfig)
    val call = if (operation.isMove) {
      api.moveUssFile(
        authorizationToken = connectionConfig.authToken,
        body = MoveUssFile(
          from = from
        ),
        filePath = FilePath(
          path = to
        )
      )
    } else {
      api.copyUssFile(
        authorizationToken = connectionConfig.authToken,
        body = CopyDataUSS.CopyFromFileOrDir(
          from = from,
          overwrite = operation.forceOverwriting,
          links = CopyDataUSS.Links.ALL,
          preserve = CopyDataUSS.Preserve.ALL,
          recursive = true
        ),
        filePath = FilePath(
          path = to
        )
      )
    }
    return Triple(call.cancelByIndicator(progressIndicator), from, to)
  }

  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    for ((requester, _) in operation.commonUrls(dataOpsManager)) {
      try {
        val (call, from, to) = makeCall(requester.connectionConfig, operation, progressIndicator)
        val response = call.execute()
        if (!response.isSuccessful) {
          val operationName = if (operation.isMove) {
            "move"
          } else {
            "copy"
          }
          throwable = CallException(response, "Cannot $operationName $from to $to")
        }
        break
      } catch (t: Throwable) {
        throwable = t
      }
    }
    if (throwable != null) {
      throw throwable
    }
  }

}