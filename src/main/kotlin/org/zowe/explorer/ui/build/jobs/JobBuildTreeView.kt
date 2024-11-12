/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.ui.build.jobs

import com.intellij.build.BuildTreeConsoleView
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.ExecutionNode
import com.intellij.build.events.impl.*
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.icons.AllIcons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanel
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.log.JobLogFetcher
import org.zowe.explorer.dataops.log.JobProcessInfo
import org.zowe.explorer.dataops.log.MFLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.zowe.kotlinsdk.SpoolFile
import java.awt.BorderLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

val JOBS_LOG_VIEW = DataKey.create<JobBuildTreeView>("jobsLogView")
const val JOBS_LOG_NOTIFICATION_GROUP_ID = "org.zowe.explorer.explorer.ExplorerNotificationGroup"

const val SUCCESSFUL_JOB_COMPLETION_CODE = 0
const val SUCCESSFUL_JOB_COMPLETION_CODE_WITH_WARNING = 4

/**
 * Console with BuildTree for display job execution process and results.
 * @param jobLogInfo job process information necessary to get log and status.
 * @param consoleView console to log
 * @param dataOpsManager instance of dataOpsManager
 * @param workingDir working directory (in most cases, path to submitted file)
 * @param project project instance
 * @author Valentine Krus
 */
@Suppress("UnstableApiUsage")
class JobBuildTreeView(
  jobLogInfo: JobProcessInfo,
  consoleView: ConsoleView,
  dataOpsManager: DataOpsManager,
  workingDir: String = "",
  project: Project
) : ExecutionConsole, DataProvider, JBPanel<JobBuildTreeView>() {

  private val buildId = jobLogInfo.jobId ?: "UNKNOWN JOB ID"
  private val jobNameNotNull = jobLogInfo.jobName ?: "UNKNOWN JOB"
  private val connectionConfig = jobLogInfo.connectionConfig

  private val buildDescriptor = DefaultBuildDescriptor(buildId, jobNameNotNull, workingDir, Date().time)
  private val treeConsoleView = BuildTreeConsoleView(project, buildDescriptor, consoleView) { false }

  private val actionToolbarGroup: ActionGroup = ActionManager.getInstance().getAction("org.zowe.explorer.actions.JobsLogActionBarGroup") as ActionGroup
  private val place: String = "Jobs Log"
  private val actionToolbar = ActionManager.getInstance().createActionToolbar(place, actionToolbarGroup, true)

  /**
   * Spool files associated with their content.
   */
  private val spoolFileToLogMap = mutableMapOf<SpoolFile, String>()

  /**
   * MFLogger instance to display job process log.
   */
  private val jobLogger = dataOpsManager.createMFLogger<JobProcessInfo, JobLogFetcher>(jobLogInfo, consoleView)

  init {
    Disposer.register(this, consoleView)
    Disposer.register(this, treeConsoleView)
    layout = BorderLayout()
    add(this.component, BorderLayout.CENTER)
  }

  /**
   * Starts fetching log via jobLogger to console view and update component UI.
   */
  fun start() {
    treeConsoleView.onEvent(buildId, StartBuildEventImpl(buildDescriptor, buildId))
    actionToolbar.let {
      it.targetComponent = treeConsoleView.component
      treeConsoleView.component.add(it.component, BorderLayout.PAGE_START)
    }

    jobLogger.startLogging()

    fun onNextLog() {
      val cachedSpoolLog = jobLogger.logFetcher.getCachedLog()
      if (cachedSpoolLog.count() != spoolFileToLogMap.count()) {
        cachedSpoolLog.minus(spoolFileToLogMap.keys).forEach {
          treeConsoleView.onEvent(buildId, StartEventImpl(it.key.id, buildId, Date().time, it.key.ddName))
        }
        cachedSpoolLog.forEach {
          val prevLog = spoolFileToLogMap[it.key] ?: ""
          val logToDisplay = if (it.value.length >= prevLog.length) it.value.substring(prevLog.length) else prevLog
          treeConsoleView.onEvent(buildId, OutputBuildEventImpl(it.key.id, logToDisplay, true))
          spoolFileToLogMap[it.key] = it.value
        }
      }
    }

    jobLogger.onNextLog {
      onNextLog()
    }

    jobLogger.onLogFinished {
      val rc = jobLogger
        .logFetcher
        .getCachedJobStatus()
        ?.returnedCode
        ?.uppercase()
      //Variables were added to set the correct icon depending on the result of the job execution.
      //For any execution status, FailureResultImpl will be used to display DDs
      val ret = if (rc?.contains("CC") == true) { // result code can be in format "CC nnnn"
        val completionCode = rc.split(" ")[1].toInt()
        when (completionCode) {
          SUCCESSFUL_JOB_COMPLETION_CODE -> ReturnCode.SUCCESS
          SUCCESSFUL_JOB_COMPLETION_CODE_WITH_WARNING -> ReturnCode.WARNING
          else -> ReturnCode.ERROR
        }
      } else ReturnCode.ERROR

      jobLogger.fetchLog()
      var finalLogFiles = jobLogger.logFetcher.getCachedLog()
      if (finalLogFiles.count() != spoolFileToLogMap.count()) {
        onNextLog()
      }

      runBlocking {
        //TODO Need to be reworked
        //It is possible that not all DDs are displayed (but information about them already exists),
        // And, accordingly, the correct icon cannot be set for them.
        //Sleep for 1 second to wait for display
        if ((treeConsoleView.tree.model.root as DefaultMutableTreeNode).firstChild.childCount < spoolFileToLogMap.count())
          delay(1000)

        setIconRec(
          (treeConsoleView.tree.model.root as DefaultMutableTreeNode).firstChild,
          ret
        )
      }

      finalLogFiles
        .forEach {
          treeConsoleView.onEvent(
            buildId,
            FinishEventImpl(it.key.id, buildId, Date().time, it.key.ddName, FailureResultImpl())
          )
        }
      treeConsoleView.onEvent(
        buildId,
        FinishBuildEventImpl(buildId, buildId, Date().time, buildId, FailureResultImpl())
      )
    }
  }

  /**
   * The function recursively sets the job execution status icon
   */
  private fun setIconRec(buildNode: TreeNode, ret: ReturnCode) {
    setIcon(buildNode, ret)
    if (buildNode.childCount > 0)
      for (currChild in buildNode.children()) {
        setIcon(currChild, ret)
      }
  }

  /**
   * The function  sets the job execution status icon
   */
  private fun setIcon(buildNode: TreeNode, ret: ReturnCode) {
    val buildExecutionNode = (buildNode as DefaultMutableTreeNode).userObject as ExecutionNode
    when (ret) {
      ReturnCode.ERROR -> buildExecutionNode.setIconProvider { AllIcons.General.BalloonError }
      ReturnCode.WARNING -> buildExecutionNode.setIconProvider { AllIcons.General.BalloonWarning }
      ReturnCode.SUCCESS -> buildExecutionNode.setIconProvider { AllIcons.General.InspectionsOK }
    }
  }

  /**
   * Enum for job execution status
   */
  enum class ReturnCode {
    SUCCESS, WARNING, ERROR
  }


  /**
   * Stops requesting logs from mainframe.
   */
  fun stop() {
    jobLogger.stopLogging()
  }

  fun getJobLogger(): MFLogger<JobLogFetcher> {
    return jobLogger
  }

  fun getConnectionConfig(): ConnectionConfig {
    return connectionConfig
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

  override fun getData(dataId: String): Any? {
    return when {
      JOBS_LOG_VIEW.`is`(dataId) -> this
      else -> null
    }
  }

  fun showNotification(title: String, content: String, project: Project?, type: NotificationType) {
    Notification(
      JOBS_LOG_NOTIFICATION_GROUP_ID,
      title,
      content,
      type,
    ).let {
      Notifications.Bus.notify(it, project)
    }
  }

}
