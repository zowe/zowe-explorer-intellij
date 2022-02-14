package eu.ibagroup.formainframe.explorer.ui.jobs

import com.intellij.build.BuildTreeConsoleView
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.*
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.JobLogInfo
import eu.ibagroup.r2z.SpoolFile
import java.util.*
import javax.swing.JComponent

class JobBuildTreeView(
  jobLogInfo: JobLogInfo,
  consoleView: ConsoleView,
  dataOpsManager: DataOpsManager,
  workingDir: String = "",
  project: Project
): ExecutionConsole {

  private val buildId = jobLogInfo.jobId ?: "UNKNOWN JOB ID"
  private val jobNameNotNull = jobLogInfo.jobName ?: "UNKNOWN JOB"
  private val buildDescriptor = DefaultBuildDescriptor(buildId, jobNameNotNull, workingDir, Date().time)
  private val treeConsoleView = BuildTreeConsoleView(project, buildDescriptor, consoleView) { false }
  private val spoolFileToLogMap = mutableMapOf<SpoolFile, String>()
  private val jobLogger = dataOpsManager.createMFLogger<JobLogInfo, JobLogFetcher>(jobLogInfo, consoleView)

  init {
    Disposer.register(this, consoleView)
    Disposer.register(this, treeConsoleView)
  }

  fun start() {
    treeConsoleView.onEvent(buildId, StartBuildEventImpl(buildDescriptor, buildId))

    jobLogger.onNextLog {
      val cachedSpoolLog = jobLogger.logFetcher.getCachedLog()
      cachedSpoolLog
        .minus(spoolFileToLogMap.keys)
        .forEach {
          treeConsoleView.onEvent(buildId, StartEventImpl(it.key.id, buildId, Date().time, it.key.ddName))
        }
      cachedSpoolLog
        .forEach {
          val prevLog = spoolFileToLogMap[it.key] ?: ""
          val logToDisplay = it.value.substring(prevLog.length)
          treeConsoleView.onEvent(buildId, OutputBuildEventImpl(it.key.id, logToDisplay, true))
          spoolFileToLogMap[it.key] = it.value
        }
    }

    jobLogger.onLogFinished {
      val result = if (jobLogger
          .logFetcher
          .getCachedJobStatus()
          ?.returnedCode
          ?.toUpperCase()
          ?.contains("ERR") == true
      ) FailureResultImpl() else SuccessResultImpl()
      jobLogger.logFetcher.getCachedLog()
        .forEach {
          treeConsoleView.onEvent(buildId, FinishEventImpl(it.key.id, buildId, Date().time, it.key.ddName, result))
        }
      treeConsoleView.onEvent(buildId, FinishBuildEventImpl(buildId, buildId, Date().time, buildId, result))
    }

    jobLogger.startLogging()
  }

  override fun dispose() {
    Disposer.dispose(this)
  }

  override fun getComponent(): JComponent {
    return treeConsoleView.component
  }

  override fun getPreferredFocusableComponent(): JComponent {
    return treeConsoleView.preferredFocusableComponent
  }

}
