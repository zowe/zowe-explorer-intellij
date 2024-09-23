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

package eu.ibagroup.formainframe.dataops.log

import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.Disposable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Base implementation of MFLogger class.
 * @author Valentine Krus
 */
abstract class AbstractMFLoggerBase<PInfo: MFProcessInfo, LFetcher: LogFetcher<PInfo>>(
  /**
   * Process characteristics that will be prompted to low-level LogFetcher.
   * Represents necessary unique process info to fetch log.
   */
  private val mfProcessInfo: PInfo,
  /**
   * Console to attach mfProcess logging.
   */
  private val consoleView: ConsoleView,
  /**
   * Maximum fetch count. Needed to limit count of requests to mainframe.
   * Logging will be stopped after request number will exceed maxFetchCount.
   */
  private val maxFetchCount: Int? = null
): MFLogger<LFetcher>, Disposable {

  /**
   * Logical representation of mainframe process.
   */
  private val processHandler: ProcessHandler

  /**
   * Indicates either the logging stopped (or will be stopped in next iteration) or not.
   */
  private val isStopped = AtomicBoolean(false)

  /**
   * Handle process finishing.
   */
  private var onFinishHandler: () -> Unit = {}

  /**
   * Handle next request for log fetching.
   */
  private var onNextLogHandler: (Array<String>) -> Unit = {}

  private var thread: Thread? = null

  init {
    processHandler = NopProcessHandler()
    consoleView.attachToProcess(processHandler)
  }

  /**
   * Performs fetching log and displaying it in console every 5 seconds with multiply factor 1.2.
   * @see MFLogger.startLoggingSync
   */
  override fun startLoggingSync() {
    var oldLog = ""
    var fetchNumber = 0
    val sleepTime: Long = 5000
    while (!isStopped.get() && (maxFetchCount == null || fetchNumber <= maxFetchCount)) {
      val newLog = logFetcher.fetchLog(mfProcessInfo).also { onNextLogHandler(it) }.joinToString("")

      if (newLog.length >= oldLog.length && newLog.substring(0, oldLog.length) == oldLog) {
        processHandler.notifyTextAvailable(newLog.substring(oldLog.length), ProcessOutputType.STDOUT)
      } else {
        consoleView.clear()
        processHandler.notifyTextAvailable(newLog, ProcessOutputType.STDOUT)
      }
      oldLog = newLog
      if (logFetcher.isLogFinished(mfProcessInfo)) {
        break
      }
      Thread.sleep(sleepTime)
      ++fetchNumber
    }
    if (!isStopped.get()) {
      processHandler.notifyTextAvailable(logFetcher.logPostfix(mfProcessInfo), ProcessOutputType.STDOUT)
    }
    processHandler.destroyProcess()
    onFinishHandler()
  }

  /**
   * Creates new thread and starts logging inside it.
   */
  override fun startLogging() {
    thread {
      startLoggingSync()
    }.also {
      thread = it
    }
  }

  /**
   * Sets stop indicator.
   */
  override fun stopLogging() {
    isStopped.set(true)
  }

  /**
   * Sets onLogFinished handler.
   */
  override fun onLogFinished(finishHandler: () -> Unit) {
    this.onFinishHandler = finishHandler
  }

  /**
   * Sets onNextLog handler.
   */
  override fun onNextLog(nextLogHandler: (Array<String>) -> Unit) {
    this.onNextLogHandler = nextLogHandler
  }

  /**
   * Stops logging.
   */
  override fun dispose() {
    stopLogging()
    thread?.join()
  }

}
