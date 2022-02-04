package eu.ibagroup.formainframe.explorer.ui.jobs

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.jeslog.JobLogProvider
import eu.ibagroup.formainframe.dataops.jeslog.MFLogger
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.r2z.SubmitJobRequest

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

            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            val jobLogger = MFLogger(JobLogProvider(connectionConfig, jobRequest), consoleView)

            val content = contentManager.factory.createContent(consoleView.component, mfFilePath, false)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
            contentManager.canCloseContents()

            jobLogger.startLogging()
          }
        }
      }
    )
  }
}
