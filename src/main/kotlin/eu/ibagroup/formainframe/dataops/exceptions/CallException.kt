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

package eu.ibagroup.formainframe.dataops.exceptions

import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.gson
import retrofit2.Response

/** The map contains the correspondence between the response message and the message for the user. */
val responseMessageMap = mapOf(
  Pair("Unauthorized", "Credentials are not valid"),
  Pair("Not Found", "Endpoint not found")
)

/**
 * Generating an exception message string.
 * @param code exception code.
 * @param message exception message.
 * @param errorParams additional exception information.
 * @return prepared exception message.
 */
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

/**
 * Class which represents exceptions with own handling implementation.
 * @param code exception code.
 * @param headMessage head exception message.
 * @param errorParams additional exception information.
 * @param cause thrown exception.
 */
class CallException(
  val code: Int,
  val headMessage: String,
  val errorParams: Map<*, *>? = null,
  override val cause: Throwable? = null
) : Exception(formatMessage(code, headMessage, errorParams))

/**
 * Generating an exception [CallException] from the response.
 * @param response api request response.
 * @param headMessage head exception message.
 * @return generated exception [CallException].
 */
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
