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
import eu.ibagroup.r2z.FilePath
import eu.ibagroup.r2z.MoveUssFile
import eu.ibagroup.r2z.RenameData


class RenamePrepared<Attr : VFileInfoAttributes>(
  val newName: String,
  val fileToRename: VirtualFile,
  val attributes: Attr
)

class RenameRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*> {
    return RenameOperationRunner(dataOpsManager)
  }
}

class RenameOperationRunner(dataOpsManager: DataOpsManager) : OperationRunnerBase<RenameOperation, RenamePrepared<*>>(
  dataOpsManager
) {
  override val operationClass = RenameOperation::class.java
  override val tasksProvider = object : TasksProvider<RenameOperation, RenamePrepared<*>, Unit, Unit> {
    override fun makeTaskTitle(operation: RenameOperation): String {
      return "Renaming File"
    }

    override fun combineResults(list: List<Unit>) {

    }

    override fun subtaskTitle(parameter: RenamePrepared<*>): String {
      return "Renaming ${parameter.fileToRename.name} to ${parameter.newName}"
    }

    override fun run(parameter: RenamePrepared<*>, progressIndicator: ProgressIndicator) {
      val attributes = parameter.attributes
      if (attributes is RemoteDatasetAttributes) {
        attributes.requesters.stream().map {
          try {
            val response = api<DataAPI>(it.connectionConfig).renameDataset(
              authorizationToken = it.connectionConfig.token,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = attributes.name
                )
              ),
              toDatasetName = parameter.newName
            ).execute()
            if (response.isSuccessful) {
              runWriteActionOnWriteThread {
                parameter.fileToRename.rename(this@RenameOperationRunner, parameter.newName)
              }
              true
            } else {
              false
            }
          } catch (e: Throwable) {
            false
          }
        }.filter { it }.findAnyNullable() ?: throw UnknownError("")
      } else if (attributes is RemoteMemberAttributes) {
        val parentAttributes = dataOpsManager.tryToGetAttributes(attributes.libraryFile) as RemoteDatasetAttributes
        parentAttributes.requesters.stream().map {
          try {
            val response = api<DataAPI>(it.connectionConfig).renameDatasetMember(
              authorizationToken = it.connectionConfig.token,
              body = RenameData(
                fromDataset = RenameData.FromDataset(
                  oldDatasetName = parentAttributes.datasetInfo.name,
                  oldMemberName = attributes.memberInfo.name
                )
              ),
              toDatasetName = parentAttributes.datasetInfo.name,
              memberName = parameter.newName
            ).execute()
            if (response.isSuccessful) {
              runWriteActionOnWriteThread {
                parameter.fileToRename.rename(this@RenameOperationRunner, parameter.newName)
              }
              true
            } else {
              false
            }
          } catch (e: Throwable) {
            false
          }
        }.filter { it }.findAnyNullable() ?: throw UnknownError("")
      } else if (attributes is RemoteUssAttributes) {
        val parentDirPath = attributes.parentDirPath
        attributes.requesters.stream().map {
          try {
            val response = api<DataAPI>(it.connectionConfig).moveUssFile(
              authorizationToken = it.connectionConfig.token,
              body = MoveUssFile(
                from = attributes.path
              ),
              filePath = FilePath("$parentDirPath/${parameter.newName}")
            ).execute()
            if (response.isSuccessful) {
              runWriteActionOnWriteThread {
                parameter.fileToRename.rename(this@RenameOperationRunner, parameter.newName)
              }
              true
            } else {
              false
            }
          } catch (e: Throwable) {
            false
          }
        }.filter { it }.findAnyNullable() ?: throw UnknownError("")
      }
    }

    override val isCancellable: Boolean = false

  }


  override fun tryToPrepare(operation: RenameOperation): List<RenamePrepared<*>> {

    val attributes = dataOpsManager.tryToGetAttributes(operation.file)

    if (attributes is RemoteDatasetAttributes || attributes is RemoteMemberAttributes || attributes is RemoteUssAttributes) {
      return listOf(
        RenamePrepared(
          newName = operation.newName,
          attributes = attributes,
          fileToRename = operation.file
        )
      )
    }
    return emptyList()
  }

}