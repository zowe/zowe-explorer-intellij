package eu.ibagroup.formainframe.dataops.services

import java.util.*

class ErrorSeparatorServiceImpl : ErrorSeparatorService {
  override fun separateErrorMessage(errorMessage: String): Properties {
    val errorProperties = Properties()
    try {
      val indOfErrorCodeDelimiter = errorMessage.indexOf(' ')
      val indOfErrorPostfix = errorMessage.lastIndexOf('(')

      val errorCode = errorMessage.subSequence(0, indOfErrorCodeDelimiter)
      val description = errorMessage.subSequence(indOfErrorCodeDelimiter + 1, indOfErrorPostfix-1);
      val errorPostfix = errorMessage.subSequence(indOfErrorPostfix, errorMessage.length)

      errorProperties["error.code"] = errorCode
      errorProperties["error.description"] = description
      errorProperties["error.postfix"] = errorPostfix

      return errorProperties
    } catch (e: Exception) {
      errorProperties["error.description"] = errorMessage
    }
    return errorProperties
  }
}
