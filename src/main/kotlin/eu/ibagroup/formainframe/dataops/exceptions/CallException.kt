package eu.ibagroup.formainframe.dataops.exceptions

import eu.ibagroup.formainframe.utils.gson
import retrofit2.Response

class CallException(
  val code: Int,
  override val message: String,
  private val errorParams: Map<*, *>? = null,
  override val cause: Throwable? = null
) : Exception(message) {
  override fun toString(): String {
    val message = "Code: $code"
    return if (errorParams != null) {
      message + "\nCATEGORY: ${errorParams["category"]}\n" +
        "MESSAGE: ${errorParams["message"]}\n" +
        "RETURN CODE: ${errorParams["rc"]}\n" +
        "REASON: ${errorParams["reason"]}\n" +
        "STACK:\n${errorParams["stack"]}"
    } else {
      message
    }
  }
}

fun CallException(response: Response<*>, message: String): CallException {
  return try {
    CallException(
      code = response.code(),
      message = message,
      errorParams = gson.fromJson(response.errorBody()?.charStream(), Map::class.java)
    )
  } catch (t : Throwable) {
    CallException(
      code = response.code(),
      message = message,
      cause = t
    )
  }
}
