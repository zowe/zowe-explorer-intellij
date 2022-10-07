/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
<<<<<<<< HEAD:src/main/kotlin/org/zowe/explorer/dataops/operations/DefaultFileMover.kt

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.Requester
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.findAnyNullable
========
package eu.ibagroup.formainframe.dataops.operations.mover

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.Requester
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.findAnyNullable
>>>>>>>> release/v0.7.0:src/main/kotlin/org/zowe/explorer/dataops/operations/mover/DefaultFileMover.kt
import retrofit2.Call
import java.io.IOException

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
    requesterWithUrl: Pair<Requester, ConnectionConfig>
  ): Call<Void>

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
