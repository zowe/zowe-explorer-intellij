package eu.ibagroup.formainframe.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.fetch.LibraryQuery
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.*
import retrofit2.Response

// TODO: doc Valiantsin
abstract class AbstractPdsToUssFolderMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  abstract fun copyMember(
    operation: MoveCopyOperation,
    libraryAttributes: RemoteDatasetAttributes,
    memberName: String,
    sourceConnectionConfig: ConnectionConfig,
    destinationPath: String,
    destConnectionConfig: ConnectionConfig,
    progressIndicator: ProgressIndicator
  ): Response<*>?

  private fun rollback(
    prevResponse: Response<*>? = null,
    sourceName: String,
    destinationPath: String,
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val opName = if (operation.isMove) "move" else "copy"
    var throwable: Throwable? = null
    val isCanceled = progressIndicator.isCanceled
    if (!isCanceled) {
      val msg = "Cannot $opName to $destinationPath."
      throwable = if (prevResponse == null) Exception(msg) else CallException(prevResponse, msg)
    }
    progressIndicator.text = "Attempt to rollback"
    val responseRollback = api<DataAPI>(connectionConfig).deleteUssFile(
      connectionConfig.authToken,
      destinationPath.substring(1),
      XIBMOption.RECURSIVE
    ).execute()

    if (isCanceled && !responseRollback.isSuccessful) {
      throwable = CallException(responseRollback, "Cannot rollback $opName $sourceName to $sourceName.")
    } else if (!isCanceled && responseRollback.isSuccessful) {
      val msg = "Cannot $opName $sourceName to $destinationPath. Rollback proceeded successfully."
      throwable = if (prevResponse == null) Exception() else CallException(prevResponse, msg)
    } else if (!isCanceled && !responseRollback.isSuccessful) {
      throwable = CallException(responseRollback, "Cannot $opName $sourceName to ${destinationPath}. Rollback failed.")
    }
    return throwable
  }

  fun proceedPdsMove(
    sourceConnectionConfig: ConnectionConfig,
    destConnectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    progressIndicator: ProgressIndicator
  ): Throwable? {
    val sourceAttributes = (operation.sourceAttributes as RemoteDatasetAttributes)
    val destinationAttributes = (operation.destinationAttributes as RemoteUssAttributes)

    val sourceQuery = UnitRemoteQueryImpl(LibraryQuery(operation.source as MFVirtualFile), sourceConnectionConfig)

    val sourceFileFetchProvider = dataOpsManager
      .getFileFetchProvider<LibraryQuery, RemoteQuery<LibraryQuery, Unit>, MFVirtualFile>(
        LibraryQuery::class.java, RemoteQuery::class.java, MFVirtualFile::class.java
      )

    sourceFileFetchProvider.reload(sourceQuery)
    var throwable: Throwable? = null

    if (sourceFileFetchProvider.isCacheValid(sourceQuery)) {
      val destinationPath = "${destinationAttributes.path}/${sourceAttributes.name}"

      if (operation.forceOverwriting) {
        val response = api<DataAPI>(destConnectionConfig).deleteUssFile(
          authorizationToken = destConnectionConfig.authToken,
          filePath = destinationPath.substring(1),
          xIBMOption = XIBMOption.RECURSIVE
        ).cancelByIndicator(progressIndicator).execute()
        if (!response.isSuccessful) {
          throw CallException(response, "Cannot overwrite directory '$destinationPath'.")
        }
      }

      val response = api<DataAPI>(destConnectionConfig).createUssFile(
        authorizationToken = destConnectionConfig.authToken,
        filePath = FilePath(destinationAttributes.path + "/" + sourceAttributes.name),
        body = CreateUssFile(FileType.DIR, destinationAttributes.fileMode ?: FileMode(7, 7, 7))
      ).cancelByIndicator(progressIndicator).execute()

      if (response.isSuccessful) {
        val cachedChildren = sourceFileFetchProvider.getCached(sourceQuery)

        cachedChildren?.forEach {
          var responseCopyMember: Response<*>? = null
          runCatching {
            responseCopyMember = copyMember(
              operation,
              sourceAttributes,
              it.name,
              sourceConnectionConfig,
              destinationPath,
              destConnectionConfig,
              progressIndicator
            )
          }
          if (progressIndicator.isCanceled || responseCopyMember?.isSuccessful != true) {
            throwable = rollback(
              responseCopyMember,
              "${sourceAttributes.name}(${it.name})",
              destinationPath,
              destConnectionConfig,
              operation,
              progressIndicator
            )
          }
          progressIndicator.checkCanceled()
        }
      }
    }

    if (operation.isMove) {
      runCatching {
        dataOpsManager.performOperation(DeleteOperation(operation.source, sourceAttributes))
      }
    }

    return throwable
  }

}