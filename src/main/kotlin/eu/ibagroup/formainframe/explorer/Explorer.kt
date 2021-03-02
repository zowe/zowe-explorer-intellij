package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.utils.castOrNull

val globalExplorer
  get() = Explorer.appInstance

interface ExplorerListener {
  fun onChanged(explorer: Explorer, unit: ExplorerUnit) {}
  fun onAdded(explorer: Explorer, unit: ExplorerUnit) {}
  fun onDeleted(explorer: Explorer, unit: ExplorerUnit) {}
}

interface Explorer {

  companion object {
    @JvmStatic
    val appInstance: Explorer
      get() = ApplicationManager.getApplication().getService(Explorer::class.java)

    @JvmStatic
    val UNITS_CHANGED = Topic.create("unitsChanged", ExplorerListener::class.java)
  }

  val units: Collection<ExplorerUnit>

  fun disposeUnit(unit: ExplorerUnit)

  fun isUnitPresented(unit: ExplorerUnit): Boolean

  val componentManager: ComponentManager

  val nullableProject: Project?
    get() = componentManager.castOrNull()

}