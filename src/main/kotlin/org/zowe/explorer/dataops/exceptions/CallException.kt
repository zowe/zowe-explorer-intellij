/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.exceptions

import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.gson
import retrofit2.Response
import java.io.IOException

private fun formatMessage(code: Int, message: String, errorParams: Map<*, *>?): String {
  val result = "$message\nCode: $code"
  return if (errorParams != null) {
    result + "\nCATEGORY: ${errorParams["category"]}\n" +
      "MESSAGE: ${errorParams["message"]}\n" +
      "RETURN CODE: ${errorParams["rc"]}\n" +
      "DETAILS:\n${errorParams["details"]?.castOrNull<List<String>>()?.joinToString("\n")}\n" +
      "REASON: ${errorParams["reason"]}"
  } else {
    result
  }
}

class CallException(
  val code: Int,
  headMessage: String,
  val errorParams: Map<*, *>? = null,
  override val cause: Throwable? = null
) : Exception(formatMessage(code, headMessage, errorParams))

fun CallException(response: Response<*>, headMessage: String): CallException {
  return try {
    CallException(
      code = response.code(),
      headMessage = headMessage,
      errorParams = gson.fromJson(response.errorBody()?.charStream(), Map::class.java)
    )
  } catch (t: Throwable) {
    CallException(
      code = response.code(),
      headMessage = headMessage,
      cause = t
    )
  }
}
