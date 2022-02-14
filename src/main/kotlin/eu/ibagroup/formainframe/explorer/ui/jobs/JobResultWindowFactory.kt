package eu.ibagroup.formainframe.explorer.ui.jobs

import com.intellij.build.*
import com.intellij.build.events.impl.*
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.AbstractMFLoggerBase
import eu.ibagroup.formainframe.dataops.log.JobLogInfo
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.r2z.SpoolFile
import eu.ibagroup.r2z.SubmitJobRequest
import java.util.*

interface JobHandler {
  fun submitted(connectionConfig: ConnectionConfig, mfFilePath: String, jobRequest: SubmitJobRequest)
}

@JvmField
val JOB_ADDED_TOPIC = Topic.create("jobAdded", JobHandler::class.java)

class JobResultWindowFactory: ToolWindowFactory {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.hide()
    subscribe(
      JOB_ADDED_TOPIC,
      object: JobHandler {
        override fun submitted(connectionConfig: ConnectionConfig, mfFilePath: String, jobRequest: SubmitJobRequest) {
          runInEdt {
            toolWindow.setAvailable(true, null)
            toolWindow.activate(null)
            toolWindow.show(null)
            val contentManager = toolWindow.contentManager

            val jobBuildTreeView = JobBuildTreeView(
              JobLogInfo(jobRequest.jobid, jobRequest.jobname, connectionConfig),
              BuildTextConsoleView(project, true, emptyList()),
              service(),
              mfFilePath,
              project
            )
            Disposer.register(toolWindow.disposable, jobBuildTreeView)

            val content = contentManager.factory.createContent(jobBuildTreeView.component, mfFilePath, false)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)

            jobBuildTreeView.start()
          }
        }
      }
    )
  }
}
