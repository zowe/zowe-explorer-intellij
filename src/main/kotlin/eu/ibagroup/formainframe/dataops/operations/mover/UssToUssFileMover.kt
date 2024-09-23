/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */
package eu.ibagroup.formainframe.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.USS_DELIMITER
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.DeleteOperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.OperationRunnerFactory
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.getParentsChain
import eu.ibagroup.formainframe.utils.log
import org.zowe.kotlinsdk.CopyDataUSS
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import retrofit2.Call
import retrofit2.Response

/**
 * Factory for registering UssToUssFileMover in Intellij IoC container
 * @see UssToUssFileMover
 */
class UssToUssFileMoverFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return UssToUssFileMover(dataOpsManager)
  }
}

/**
 * Implements copying of uss file to uss directory inside 1 system
 */
class UssToUssFileMover(private val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  override fun canRun(operation: MoveCopyOperation): Boolean {
    return operation.sourceAttributes is RemoteUssAttributes
            && operation.destinationAttributes is RemoteUssAttributes
            && operation.destinationAttributes.isDirectory
            && operation.commonUrls(dataOpsManager).isNotEmpty()
            && !operation.destination.getParentsChain().containsAll(operation.source.getParentsChain())
  }

  override val log = log<UssToUssFileMover>()

  /**
   * Proceeds move/copy of uss file to uss directory
   * @param connectionConfig connection configuration of system inside which to copy file
   * @param operation requested operation
   */
  private fun makeCall(
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation
  ): Triple<Pair<Call<Void>, () -> Unit>, String, String> {
    val sourceAttributes = (operation.sourceAttributes as RemoteUssAttributes)
    val from = sourceAttributes.path
    val to = computeUssDestination(operation)
    val call = if (operation.isMove)
      buildMoveCall(connectionConfig, operation, from, to)
    else
      buildCopyCall(connectionConfig, operation, from, to)
    return Triple(call, from, to)
  }

  /**
   * Function builds a Move call and Delete source callback after moving is performed
   * @return Pair of Move call and its delete source file callback
   */
  private fun buildMoveCall(
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    from: String,
    to: String)
  : Pair<Call<Void>, () -> Unit> {
    val copyCall = buildCopyCall(connectionConfig, operation, from, to).first
    val deleteSourceCallback = buildDeleteSourceCallback(operation)
    return Pair(copyCall, deleteSourceCallback)
  }

  /**
   * Function builds a Copy call
   * @return Pair of Copy call and empty callback function to execute after Copy is performed
   */
  private fun buildCopyCall(
    connectionConfig: ConnectionConfig,
    operation: MoveCopyOperation,
    from: String,
    to: String)
  : Pair<Call<Void>, () -> Unit> {
    return Pair(
      api<DataAPI>(connectionConfig).copyUssFile(
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
    ) {}
  }

  /**
   * Function builds a Delete callback which will be executed after successful Move is performed
   * @return delete callback
   */
  private fun buildDeleteSourceCallback(operation: MoveCopyOperation): () -> Unit {
    return {
      val sourceAttributes = operation.sourceAttributes as RemoteUssAttributes
      val deleteOperation = DeleteOperation(operation.source, sourceAttributes)
      DeleteOperationRunner(dataOpsManager).run(deleteOperation)
    }
  }

  /**
   * Function is used to determine the correct USS destination for Move/Copy operation
   * The possible list of destinations are:
   *
   * Copying:
   * 1. Directory -> Directory without(with) conflict: Destination would be <ROOT_PATH>
   * 2. Directory -> Directory with conflict, but "Use new name" option was pressed: Destination would be <ROOT_PATH>/<NEW_DIR_NAME>
   * 3. File -> Directory with conflict: Destination would be <ROOT_PATH>/<FILE_NEW_NAME>
   * 4. File -> Directory without conflict: Destination would be <ROOT_PATH>/<SOURCE_FILE_NAME>
   *
   * Moving:
   * 1. Directory -> Directory without(with) conflict: Destination would be <ROOT_PATH>
   * 2. Directory -> Directory with conflict and "Use new name" is pressed: Destination would be <ROOT_PATH>/<NEW_DIR_NAME>
   * 3. File -> Directory the same behavior as for Copying
   *
   *    *   example: mv(cp) -Rf /u/<USER>/test /u/<USER>/destination
   *    *   (if test is present under destination all files and subdirs from /u/<USER>/test will be copied/moved
   *    *   and overwritten in /u/<USER>/destination/test, otherwise test would be copied/moved to /u/<USER>/destination/test)
   *    *   If operation is Move, the source would be deleted afterward
   *
   * @return target destination in String format
   */
  private fun computeUssDestination(operation: MoveCopyOperation) : String {
    val destinationRootPath = (operation.destinationAttributes as RemoteUssAttributes).path
    val destinationNewName = operation.newName
    val destinationNewNameWithDelimiter = USS_DELIMITER + operation.newName
    // Copying or Moving USS directory
    return if (operation.source.isDirectory && operation.destination.isDirectory) {
        destinationRootPath + if (destinationNewName != null) destinationNewNameWithDelimiter else ""
    }
    // Copying or Moving USS file
    else destinationRootPath + USS_DELIMITER + (operation.newName ?: operation.sourceAttributes?.name)
  }

  /**
   * Starts operation execution. Throws throwable if something went wrong
   * @throws Throwable
   * @see OperationRunner.run
   */
  override fun run(operation: MoveCopyOperation, progressIndicator: ProgressIndicator) {
    var throwable: Throwable? = null
    for ((requester, _) in operation.commonUrls(dataOpsManager)) {
      try {
        val (call, from, to) = makeCall(requester.connectionConfig, operation)
        val operationName = if (operation.isMove) "move" else "copy"
        val response: Response<Void> = call.first.cancelByIndicator(progressIndicator).execute()
        if (!response.isSuccessful) {
          throwable = CallException(response, "Cannot $operationName $from to $to")
        } else {
          // Call the built early callback for source file/dir deletion (always empty callback for Copy)
          call.second.invoke()
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
