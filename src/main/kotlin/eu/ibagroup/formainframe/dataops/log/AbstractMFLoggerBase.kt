package eu.ibagroup.formainframe.dataops.log

import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

abstract class AbstractMFLoggerBase<LInfo: LogInfo, LFetcher: LogFetcher<LInfo>>(
  private val logInfo: LInfo,
  private val consoleView: ConsoleView,
  private val maxFetchCount: Int? = null
): MFLogger<LFetcher>, Disposable {

  private val processHandler: ProcessHandler
  private val isStopped = AtomicBoolean(false)
  private var onFinishHandler: () -> Unit = {}
  private var onNextLogHandler: (Array<String>) -> Unit = {}
  private var thread: Thread? = null

  init {
    processHandler = NopProcessHandler()
    consoleView.attachToProcess(processHandler)
  }

  override fun startLoggingSync() {
    var oldLog = ""
    var fetchNumber = 0
    var sleepTime: Long = 5000
    val sleepMulFactor = 1.2
    while (!isStopped.get() && (maxFetchCount == null || fetchNumber <= maxFetchCount)) {
      val newLog = logFetcher.fetchLog(logInfo).also { onNextLogHandler(it) }.joinToString("")

      if (newLog.substring(0, oldLog.length) == oldLog) {
        processHandler.notifyTextAvailable(newLog.substring(oldLog.length), ProcessOutputType.STDOUT)
      } else {
        consoleView.clear()
        processHandler.notifyTextAvailable(newLog, ProcessOutputType.STDOUT)
      }
      oldLog = newLog
      if (logFetcher.isLogFinished(logInfo)) {
        break
      }
      Thread.sleep(sleepTime)
      sleepTime *= sleepMulFactor.toLong()
      ++fetchNumber
    }
    if (!isStopped.get()) {
      processHandler.notifyTextAvailable(logFetcher.logPostfix(logInfo), ProcessOutputType.STDOUT)
    }
    processHandler.destroyProcess()
    onFinishHandler()
  }

  override fun startLogging() {
    thread {
      startLoggingSync()
    }.also {
      thread = it
    }
  }

  override fun stopLogging() {
    isStopped.set(true)
  }

  override fun onLogFinished(finishHandler: () -> Unit) {
    this.onFinishHandler = finishHandler
  }

  override fun onNextLog(nextLogHandler: (Array<String>) -> Unit) {
    this.onNextLogHandler = nextLogHandler
  }

  override fun dispose() {
    stopLogging()
    thread?.join()
  }

}
