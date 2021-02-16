package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

val globalExplorer
  get() = Explorer.appInstance

interface Explorer {

  companion object {
    @JvmStatic
    val appInstance: Explorer
      get() = ApplicationManager.getApplication().getService(Explorer::class.java)
  }

  val project: Project?

  val units: Collection<ExplorerUnit>

}