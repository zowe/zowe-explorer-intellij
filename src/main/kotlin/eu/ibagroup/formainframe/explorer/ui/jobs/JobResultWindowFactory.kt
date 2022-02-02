package eu.ibagroup.formainframe.explorer.ui.jobs

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.r2z.SubmitJobRequest
import kotlin.concurrent.thread

interface JobHandler {
  fun submitted(jobRequest: SubmitJobRequest)
}

@JvmField
val JOB_ADDED_TOPIC = Topic.create("jobAdded", JobHandler::class.java)

class JobResultWindowFactory: ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.hide()
    subscribe(
      JOB_ADDED_TOPIC,
      object: JobHandler {
        override fun submitted(jobRequest: SubmitJobRequest) {
          runInEdt {
            toolWindow.setAvailable(true, null)
            toolWindow.activate(null)
            toolWindow.show(null)
            val contentManager = toolWindow.contentManager

            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            val processHandler = NopProcessHandler()
            consoleView.attachToProcess(processHandler)
            thread {
              Thread.sleep(2000)
              consoleView.clear()
              processHandler.notifyTextAvailable("hello, world\n", ProcessOutputType.STDOUT)
            }
            processHandler.notifyTextAvailable("hello\n", ProcessOutputType.STDOUT)
            val content = contentManager.factory.createContent(consoleView.component, "Job", false)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
            contentManager.canCloseContents()
          }
        }
      }
    )
  }
}
