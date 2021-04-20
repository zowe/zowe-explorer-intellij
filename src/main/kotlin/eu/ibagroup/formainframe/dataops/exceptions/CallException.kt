package eu.ibagroup.formainframe.dataops.exceptions

import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.gson
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
