package eu.ibagroup.formainframe.dataops.services

import java.util.*

class ErrorSeparatorServiceImpl : ErrorSeparatorService {
  override fun separateErrorMessage(errorMessage: String): Properties {
    val indOfErrorCodeDelimiter = errorMessage.indexOf(' ')
    val indOfErrorPostfix = errorMessage.lastIndexOf('(')

    val errorCode = errorMessage.subSequence(0, indOfErrorCodeDelimiter)
    val description = errorMessage.subSequence(indOfErrorCodeDelimiter + 1, indOfErrorPostfix-1);
    val errorPostfix = errorMessage.subSequence(indOfErrorPostfix, errorMessage.length)

    val errorProperties = Properties()
    errorProperties["error.code"] = errorCode
    errorProperties["error.description"] = description
    errorProperties["error.postfix"] = errorPostfix

    return errorProperties
  }
}