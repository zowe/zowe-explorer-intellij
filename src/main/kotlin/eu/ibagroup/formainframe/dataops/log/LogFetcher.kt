package eu.ibagroup.formainframe.dataops.log

import com.intellij.openapi.extensions.ExtensionPointName

interface LogFetcher<LInfo: LogInfo> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<LogFetcherFactory>("eu.ibagroup.formainframe.logFetcher")
  }

  fun fetchLog(logInfo: LInfo): Array<String>
  fun isLogFinished(logInfo: LInfo): Boolean
  fun logPostfix(logInfo: LInfo): String = ""
  val logInfoClass: Class<out LogInfo>
}
