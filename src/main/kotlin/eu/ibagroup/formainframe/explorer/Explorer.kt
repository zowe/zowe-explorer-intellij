package eu.ibagroup.formainframe.explorer

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.utils.castOrNull



interface ExplorerListener {
  fun onChanged(explorer: Explorer, unit: ExplorerUnit) {}
  fun onAdded(explorer: Explorer, unit: ExplorerUnit) {}
  fun onDeleted(explorer: Explorer, unit: ExplorerUnit) {}
}

@JvmField
val UNITS_CHANGED = Topic.create("unitsChanged", ExplorerListener::class.java)

interface ExplorerFactory<E : Explorer>  {
  fun buildComponent() : E
}

interface Explorer {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ExplorerFactory<*>>("eu.ibagroup.formainframe.explorer")
  }

  val units: Collection<ExplorerUnit>

  fun disposeUnit(unit: ExplorerUnit)

  fun isUnitPresented(unit: ExplorerUnit): Boolean

  val componentManager: ComponentManager

  val nullableProject: Project?
    get() = componentManager.castOrNull()

  fun reportThrowable(t: Throwable, project: Project?)

  fun showNotification(
    title: String,
    content: String,
    type: NotificationType = NotificationType.INFORMATION,
    project: Project?
  )

  fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?)

}