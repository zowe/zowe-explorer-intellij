package eu.ibagroup.formainframe.dataops.services

import com.intellij.openapi.application.ApplicationManager
import java.util.*

interface ErrorSeparatorService {
  companion object {
    @JvmStatic
    val instance: ErrorSeparatorService
      get() = ApplicationManager.getApplication().getService(ErrorSeparatorService::class.java)
  }

  fun separateErrorMessage(errorMessage: String): Properties
}