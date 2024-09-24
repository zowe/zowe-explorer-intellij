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

package org.zowe.explorer.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.Requester
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.DeleteOperation
import org.zowe.explorer.dataops.operations.OperationRunner
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.findAnyNullable
import org.zowe.explorer.utils.log
import retrofit2.Call

/**
 * Abstract class that wraps logic of copying/moving of files inside
 * 1 system that could be done by sending 1 request to zosmf.
 * @author Valiantsin Krus
 */
abstract class DefaultFileMover(protected val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  /**
   * Implementation should build retrofit Call to zosmf that will
   * copy/move required file right after its execution started.
   * @param operation requested operation.
   * @param requesterWithUrl requester for listing files (JobsRequester, MaskedRequester, UssRequester).
   * @return built call.
   */
  abstract fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester<ConnectionConfig>, ConnectionConfig>
  ): Call<Void>

  override val log = log<DefaultFileMover>()

  /**
   * Starts operation execution. Throws throwable if something went wrong.
   * @throws Throwable
   * @see OperationRunner.run
   */
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
        val operationMessage = if (operation.isMove) "move" else "copy"
        if (!it.isSuccessful) {
          throw CallException(it, "Cannot $operationMessage ${operation.source.name} to ${operation.destination.name}")
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
