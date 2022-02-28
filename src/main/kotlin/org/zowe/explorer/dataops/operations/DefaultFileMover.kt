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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.Requester
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.findAnyNullable
import retrofit2.Call
import java.io.IOException

abstract class DefaultFileMover(protected val dataOpsManager: DataOpsManager) : AbstractFileMover() {

  abstract fun buildCall(
    operation: MoveCopyOperation,
    requesterWithUrl: Pair<Requester, ConnectionConfig>
  ): Call<Void>


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
