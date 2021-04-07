package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.utils.castOrNull

val globalExplorer
  get() = Explorer.instance

interface ExplorerListener {
  fun onChanged(explorer: Explorer, unit: ExplorerUnit) {}
  fun onAdded(explorer: Explorer, unit: ExplorerUnit) {}
  fun onDeleted(explorer: Explorer, unit: ExplorerUnit) {}
}

@JvmField
val UNITS_CHANGED = Topic.create("unitsChanged", ExplorerListener::class.java)

interface Explorer {

  companion object {
    @JvmStatic
    val instance: Explorer
      get() = ApplicationManager.getApplication().getService(Explorer::class.java)
  }

  val units: Collection<ExplorerUnit>

  fun disposeUnit(unit: ExplorerUnit)

  fun isUnitPresented(unit: ExplorerUnit): Boolean

  val componentManager: ComponentManager

  val nullableProject: Project?
    get() = componentManager.castOrNull()

  fun reportThrowable(t: Throwable, project: Project?)

  fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?)

}