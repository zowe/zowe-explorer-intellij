package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.r2z.DataAPI
import java.io.IOException

class DeletePrepared<Attr : VFileInfoAttributes>(
  val fileToDelete: VirtualFile,
  val attributes: Attr
)

class DeleteRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*> {
    return DeleteRunner(dataOpsManager)
  }
}

class DeleteRunner(dataOpsManager: DataOpsManager) :
  OperationRunnerBase<DeleteOperation, DeletePrepared<*>>(
    dataOpsManager
  ) {
  override val operationClass = DeleteOperation::class.java

  override val tasksProvider = object : TasksProvider<DeleteOperation, DeletePrepared<*>, Unit, Unit> {

    override fun makeTaskTitle(operation: DeleteOperation): String {
      return "Running deletion of ${operation.files.size} files"
    }

    override fun combineResults(list: List<Unit>) {
    }

    override fun subtaskTitle(parameter: DeletePrepared<*>): String {
      return "Deletion of ${parameter.fileToDelete.name}"
    }

    override fun run(parameter: DeletePrepared<*>, progressIndicator: ProgressIndicator) {
      when (val attr = parameter.attributes) {
        is RemoteDatasetAttributes -> {
          var throwable: Throwable? = null
          attr.requesters.stream().map {
            try {
              val response = api<DataAPI>(it.connectionConfig).deleteDataset(
                authorizationToken = it.connectionConfig.token,
                datasetName = attr.name
              ).execute()
              if (response.isSuccessful) {
                runWriteActionOnWriteThread { parameter.fileToDelete.delete(this@DeleteRunner) }
                true
              } else {
                throwable = IOException(response.code().toString())
                false
              }
            } catch (t: Throwable) {
              throwable = t
              false
            }
          }.filter { it }.findAnyNullable() ?: throw (throwable ?: Throwable("Unknown"))
        }
        is RemoteMemberAttributes -> {

        }
        is RemoteUssAttributes -> {

        }
      }
    }

    override val isCancellable = true

  }

  override fun tryToPrepare(operation: DeleteOperation): List<DeletePrepared<*>> {
    return operation.files.mapNotNull {
      val attributes = dataOpsManager.tryToGetAttributes(it)
      if (attributes is RemoteDatasetAttributes
        || attributes is RemoteMemberAttributes
        || attributes is RemoteUssAttributes
      ) {
        DeletePrepared(it, attributes)
      } else null
    }
  }

}