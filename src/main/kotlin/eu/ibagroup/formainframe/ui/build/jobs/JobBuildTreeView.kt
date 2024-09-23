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

package eu.ibagroup.formainframe.ui.build.jobs

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
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.JobProcessInfo
import eu.ibagroup.formainframe.dataops.log.MFLogger
import org.zowe.kotlinsdk.SpoolFile
import java.awt.BorderLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode

val JOBS_LOG_VIEW = DataKey.create<JobBuildTreeView>("jobsLogView")
const val JOBS_LOG_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.ExplorerNotificationGroup"

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
  val jobLogInfo: JobProcessInfo,
  consoleView: ConsoleView,
  dataOpsManager: DataOpsManager,
  workingDir: String = "",
  project: Project
) : ExecutionConsole, DataProvider, JBPanel<JobBuildTreeView>() {

  private val buildId = jobLogInfo.jobId ?: "UNKNOWN JOB ID"
  private val jobNameNotNull = jobLogInfo.jobName ?: "UNKNOWN JOB"
  private val connectionConfig = jobLogInfo.connectionConfig

  private val buildDescriptor = DefaultBuildDescriptor(buildId, jobNameNotNull, workingDir, Date().time)
  private val treeConsoleView = BuildTreeConsoleView(project, buildDescriptor, consoleView)

  private val actionToolbarGroup: ActionGroup =
    ActionManager.getInstance().getAction("eu.ibagroup.formainframe.actions.JobsLogActionBarGroup") as ActionGroup
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
          val logToDisplay = if (it.value.length >= prevLog.length) it.value.substring(prevLog.length) else prevLog
          treeConsoleView.onEvent(buildId, OutputBuildEventImpl(it.key.id, logToDisplay, true))
          spoolFileToLogMap[it.key] = it.value
        }
    }

    jobLogger.onLogFinished {
      val rc = jobLogger
        .logFetcher
        .getCachedJobStatus()
        ?.returnedCode
        ?.uppercase()
      val result = if (rc == null || rc.contains(Regex("ERR|ABEND|CANCEL"))) FailureResultImpl()
      else SuccessResultImpl()
      jobLogger.logFetcher.getCachedLog()
        .forEach {
          treeConsoleView.onEvent(buildId, FinishEventImpl(it.key.id, buildId, Date().time, it.key.ddName, result))
        }
      runCatching {
        val buildNode = (treeConsoleView.tree.model.root as DefaultMutableTreeNode).firstChild
        val buildExecutionNode = (buildNode as DefaultMutableTreeNode).userObject as ExecutionNode
        if (result is FailureResultImpl) {
          buildExecutionNode.setIconProvider { AllIcons.General.BalloonError }
        } else {
          buildExecutionNode.setIconProvider { AllIcons.General.InspectionsOK }
        }
      }
      treeConsoleView.onEvent(buildId, FinishBuildEventImpl(buildId, buildId, Date().time, buildId, result))
    }

    jobLogger.startLogging()
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
