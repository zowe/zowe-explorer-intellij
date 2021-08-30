package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.UIComponentManager

class ExplorerWindowFactory : ToolWindowFactory, DumbAware {

  override fun isApplicable(project: Project): Boolean {
    return true
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val contentFactory = ContentFactory.SERVICE.getInstance()
    UIComponentManager.INSTANCE.getExplorerContentProviders<Explorer>().forEach {
      val content = contentFactory
        .createContent(it.buildExplorerContent(toolWindow.disposable, project), it.displayName, it.isLockable)
      toolWindow.contentManager.addContent(content)
    }
  }

  override fun init(toolWindow: ToolWindow) {}

  override fun shouldBeAvailable(project: Project): Boolean {
    return true
  }
}