/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.attributes.USS_DELIMITER
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.getParentsChain
import eu.ibagroup.r2z.CopyDataUSS
import eu.ibagroup.r2z.DataAPI
import eu.ibagroup.r2z.FilePath
import eu.ibagroup.r2z.MoveUssFile
import retrofit2.Call
import retrofit2.Response

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
        val operationName = if (operation.isMove) "move" else "copy"
        val response: Response<Void> = call.execute()
        if (!response.isSuccessful) {
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
