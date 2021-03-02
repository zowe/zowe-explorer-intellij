package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.token
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.XIBMOption
import java.io.IOException

class DeletePrepared<Attr : VFileInfoAttributes>(
  val fileToDelete: VirtualFile,
  val attributes: Attr
)

class DeleteRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*> {
    return DeleteOperationRunner(dataOpsManager)
  }
}

class DeleteOperationRunner(dataOpsManager: DataOpsManager) :
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
                runWriteActionOnWriteThread { parameter.fileToDelete.delete(this@DeleteOperationRunner) }
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
          val libraryAttributes = attr.getLibraryAttributes(dataOpsManager)
          if (libraryAttributes != null) {
            var throwable: Throwable? = null
            libraryAttributes.requesters.stream().map {
              try {
                val response = api<DataAPI>(it.connectionConfig).deleteDatasetMember(
                  authorizationToken = it.connectionConfig.token,
                  datasetName = libraryAttributes.name,
                  memberName = attr.name
                ).execute()
                if (response.isSuccessful) {
                  runWriteActionOnWriteThread { parameter.fileToDelete.delete(this@DeleteOperationRunner) }
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
        }
        is RemoteUssAttributes -> {
          var throwable: Throwable? = null
          attr.requesters.stream().map {
            try {
              val response = api<DataAPI>(it.connectionConfig).deleteUssFile(
                authorizationToken = it.connectionConfig.token,
                filePath = attr.path.substring(1),
                xIBMOption = XIBMOption.RECURSIVE
              ).execute()
              if (response.isSuccessful) {
                runWriteActionOnWriteThread { parameter.fileToDelete.delete(this@DeleteOperationRunner) }
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
      }
    }

    override val isCancellable = true

  }

  override fun tryToPrepare(operation: DeleteOperation): List<DeletePrepared<*>> {
    val preparedWithAllMembers = operation.files.mapNotNull {
      val attributes = dataOpsManager.tryToGetAttributes(it)
      if (attributes is RemoteDatasetAttributes
        || attributes is RemoteMemberAttributes
        || (attributes is RemoteUssAttributes && attributes.isWritable && attributes.path != "/")
      ) {
        DeletePrepared(it, attributes)
      } else null
    }
    return preparedWithAllMembers.filterNot { attr ->
      attr.attributes is RemoteMemberAttributes && preparedWithAllMembers.any {
        it.attributes is RemoteDatasetAttributes && it.attributes == attr.attributes.getLibraryAttributes(dataOpsManager)
      }
    }
  }
}