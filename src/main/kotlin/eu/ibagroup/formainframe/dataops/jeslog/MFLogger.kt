package eu.ibagroup.formainframe.dataops.jeslog

import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import kotlin.concurrent.thread

class MFLogger(
  val logProvider: LogProvider,
  val consoleView: ConsoleView,
  val maxFetchCount: Int? = null
) {
  val processHandler: ProcessHandler

  init {
    processHandler = NopProcessHandler()
    consoleView.attachToProcess(processHandler)
  }

  fun startLoggingSync() {
    var oldLog = ""
    var fetchNumber = 0
    var sleepTime: Long = 1000
    val sleepMulFactor = 1.2
    while (maxFetchCount == null || fetchNumber <= maxFetchCount) {
      println("here")
      val newLog = logProvider.provideLog()
      if (newLog.substring(0, oldLog.length) == oldLog) {
        processHandler.notifyTextAvailable(newLog.substring(oldLog.length), ProcessOutputType.STDOUT)
      } else {
        consoleView.clear()
        processHandler.notifyTextAvailable(newLog, ProcessOutputType.STDOUT)
      }
      oldLog = newLog
      if (logProvider.isLogFinished()) {
        break
      }
      Thread.sleep(sleepTime)
      sleepTime *= sleepMulFactor.toLong()
      ++fetchNumber
    }
    processHandler.notifyTextAvailable(logProvider.logPostfix(), ProcessOutputType.STDOUT)
    processHandler.destroyProcess()
  }

  fun startLogging() {
    thread {
      startLoggingSync()
    }
  }
}
