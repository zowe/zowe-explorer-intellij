package eu.ibagroup.formainframe.dataops.log

interface MFLogger<LFetcher: LogFetcher<out LogInfo>> {
  val logFetcher: LFetcher

  fun startLoggingSync()
  fun startLogging()
  fun onLogFinished(finishHandler: () -> Unit)
  fun onNextLog(nextLogHandler: (Array<String>) -> Unit)
  fun stopLogging()
}
