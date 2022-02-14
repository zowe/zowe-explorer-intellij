package eu.ibagroup.formainframe.explorer.ui.jobs

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
import eu.ibagroup.r2z.SubmitJobRequest

interface JobHandler {
  fun submitted(connectionConfig: ConnectionConfig, mfFilePath: String, jobRequest: SubmitJobRequest)
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

      val content = contentManager.factory.createContent(jobBuildTreeView.component, mfFilePath, false)
      contentManager.addContent(content)
      contentManager.setSelectedContent(content)

      contentToBuildViewMap[content] = jobBuildTreeView
      toolWindow.addContentManagerListener(object: ContentManagerListener {
        override fun contentRemoved(event: ContentManagerEvent) {
          contentToBuildViewMap[content]?.stop()
        }
      })
      jobBuildTreeView.start()
    }
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.hide()
    subscribe(
      JOB_ADDED_TOPIC,
      object: JobHandler {
        override fun submitted(connectionConfig: ConnectionConfig, mfFilePath: String, jobRequest: SubmitJobRequest) {
          addJobBuildContentTab(project, toolWindow, connectionConfig, mfFilePath, jobRequest.jobid, jobRequest.jobname)
        }
      }
    )
  }
}
