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
import org.zowe.kotlinsdk.*
import retrofit2.Response

/**
 * Abstract class for moving and copying partitioned data set
 * to uss directory inside 1 system and between different systems.
 * @author Valiantsin Krus
 */
abstract class AbstractPdsToUssFolderMover(val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Implementation should copy specified member from pds to created uss directory.
   * @param operation requested operation instance.
   * @param libraryAttributes attributes of source pds.
   * @param memberName name of the member to copy.
   * @param sourceConnectionConfig connection configuration of the system to copy from
   *                               (the same as destConnectionConfig for copying inside 1 system).
   * @param destinationPath destination path inside which to copy member.
   * @param destConnectionConfig connection configuration of the system to copy to.
   *                             (the same as sourceConnectionConfig for copying inside 1 system).
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   * @return response of last request to zosmf or null if something went wrong.
   */
  abstract fun copyMember(
    operation: MoveCopyOperation,
    libraryAttributes: RemoteDatasetAttributes,
    memberName: String,
    sourceConnectionConfig: ConnectionConfig,
    destinationPath: String,
    destConnectionConfig: ConnectionConfig,
    progressIndicator: ProgressIndicator
  ): Response<*>?

  /**
   * Cancel changes if something went wrong in copying process.
   * @param prevResponse response of attempted request to copy member.
   * @param sourceName name of the member to be copied.
   * @param destinationPath path of created directory to copy all members of source pds to.
   * @param connectionConfig connection configuration of destination system.
   * @param operation requested operation instance.
   * @param progressIndicator indicator that will show progress of rollback in UI.
   * @return throwable if something went wrong or null otherwise.
   */
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
    log.info("Trying to rollback changes")
    val responseRollback = api<DataAPI>(connectionConfig).deleteUssFile(
      connectionConfig.authToken,
      FilePath(destinationPath),
      XIBMOption.RECURSIVE
    ).execute()

    if (isCanceled && !responseRollback.isSuccessful) {
      throwable = CallException(responseRollback, "Cannot rollback $opName $sourceName to $sourceName.")
    } else if (!isCanceled && responseRollback.isSuccessful) {
      val msg = "Cannot $opName $sourceName to $destinationPath. Rollback proceeded successfully."
      log.info("Rollback proceeded successfully")
      throwable = if (prevResponse == null) Exception() else CallException(prevResponse, msg)
    } else if (!isCanceled && !responseRollback.isSuccessful) {
      log.info("Rollback failed")
      throwable = CallException(responseRollback, "Cannot $opName $sourceName to ${destinationPath}. Rollback failed.")
    }
    return throwable
  }

  /**
   * Proceeds copying/moving of partitioned data set (pds) to uss directory.
   * @param sourceConnectionConfig connection configuration from witch to copy pds.
   * @param destConnectionConfig connection configuration to which to copy pds.
   * @param operation requested operation instance.
   * @param progressIndicator indicator that will show progress of copying/moving in UI.
   * @return throwable if something went wrong or null otherwise.
   */
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
      .getFileFetchProvider<LibraryQuery, RemoteQuery<ConnectionConfig, LibraryQuery, Unit>, MFVirtualFile>(
        LibraryQuery::class.java, RemoteQuery::class.java, MFVirtualFile::class.java
      )

    sourceFileFetchProvider.reload(sourceQuery)
    var throwable: Throwable? = null

    if (sourceFileFetchProvider.isCacheValid(sourceQuery)) {
      val destinationPath = "${destinationAttributes.path}/${sourceAttributes.name}"

      if (operation.forceOverwriting) {
        log.info("Overwriting directory $destinationPath")
        val response = api<DataAPI>(destConnectionConfig).deleteUssFile(
          authorizationToken = destConnectionConfig.authToken,
          filePath = FilePath(destinationPath),
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
