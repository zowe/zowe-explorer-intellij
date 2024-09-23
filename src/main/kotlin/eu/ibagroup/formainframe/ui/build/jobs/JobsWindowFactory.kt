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

import com.intellij.build.*
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.log.JobProcessInfo
import eu.ibagroup.formainframe.utils.subscribe
import org.zowe.kotlinsdk.Job
import org.zowe.kotlinsdk.SubmitJobRequest

interface JobHandler {
  fun submitted(project: Project, connectionConfig: ConnectionConfig, mfFilePath: String, jobRequest: SubmitJobRequest)
  fun viewed(project: Project, connectionConfig: ConnectionConfig, mfFileName: String, jobStatus: Job)
}

@JvmField
val JOB_ADDED_TOPIC = Topic.create("jobAdded", JobHandler::class.java)

/**
 * Factory for creating job build tool window.
 * @author Valentine Krus
 */
class JobsWindowFactory: ToolWindowFactory {

  val contentToBuildViewMap = mutableMapOf<Content, JobBuildTreeView>()

  /**
   * Creates a tab in a separate toolWindow with JobBuildTreeView.
   * @param project project instance.
   * @param toolWindow tool window to add a tab.
   * @param connectionConfig corresponding connection to zosmf.
   * @param mfFilePath formed mainframe path to file that was submitted as job.
   * @param jobId job identifier.
   * @param jobName job name.
   */
  fun addJobBuildContentTab(
    project: Project,
    toolWindow: ToolWindow,
    connectionConfig: ConnectionConfig,
    mfFilePath: String,
    jobId: String?,
    jobName: String?
  ) {
    runInEdt {
      toolWindow.setAvailable(true, null)
      toolWindow.activate(null)
      toolWindow.show(null)
      val contentManager = toolWindow.contentManager

      val jobBuildTreeView = JobBuildTreeView(
        JobProcessInfo(jobId, jobName, connectionConfig),
        BuildTextConsoleView(project, true, emptyList()),
        service(),
        mfFilePath,
        project
      )

      Disposer.register(toolWindow.disposable, jobBuildTreeView)

      val content = contentManager.factory.createContent(jobBuildTreeView, mfFilePath, false)
      contentManager.addContent(content)
      contentManager.setSelectedContent(content)

      contentToBuildViewMap[content] = jobBuildTreeView
      toolWindow.addContentManagerListener(object: ContentManagerListener {
        override fun contentRemoved(event: ContentManagerEvent) {
          contentToBuildViewMap[event.content]?.stop()
        }
      })
      jobBuildTreeView.start()
    }
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
  }

  override fun init(toolWindow: ToolWindow) {
    subscribe(
      project = toolWindow.project,
      topic = JOB_ADDED_TOPIC,
      handler = object: JobHandler {
        override fun submitted(project: Project, connectionConfig: ConnectionConfig, mfFilePath: String, jobRequest: SubmitJobRequest) {
          addJobBuildContentTab(project, toolWindow, connectionConfig, mfFilePath, jobRequest.jobid, jobRequest.jobname)
        }
        override fun viewed(project: Project, connectionConfig: ConnectionConfig, mfFileName: String, jobStatus: Job) {
          addJobBuildContentTab(project, toolWindow, connectionConfig, mfFileName, jobStatus.jobId, jobStatus.jobName)
        }
      }
    )
  }
}
