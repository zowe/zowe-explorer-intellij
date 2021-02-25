package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager

val globalExplorer
  get() = Explorer.appInstance

interface Explorer {

  companion object {
    @JvmStatic
    val appInstance: Explorer
      get() = ApplicationManager.getApplication().getService(Explorer::class.java)
  }

  val units: Collection<ExplorerUnit>

  val componentManager: ComponentManager

}