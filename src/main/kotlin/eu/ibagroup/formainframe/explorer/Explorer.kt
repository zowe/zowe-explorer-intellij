package eu.ibagroup.formainframe.explorer

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationBuilder
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.CONFIGS_CHANGED
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CREDENTIALS_CHANGED
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialsListener
import eu.ibagroup.formainframe.config.jobs.JobsFilter
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.*
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import kotlin.concurrent.read
import kotlin.concurrent.write


interface ExplorerListener {
  fun onChanged(explorer: Explorer<*>, unit: ExplorerUnit)
  fun onAdded(explorer: Explorer<*>, unit: ExplorerUnit)
  fun onDeleted(explorer: Explorer<*>, unit: ExplorerUnit)
}

interface ExplorerFactory<U: WorkingSet<*>, E : Explorer<U>> {
  fun buildComponent() : E
}
@JvmField
val UNITS_CHANGED = Topic.create("unitsChanged", ExplorerListener::class.java)

interface Explorer<U: WorkingSet<*>> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ExplorerFactory<*, *>>("eu.ibagroup.formainframe.explorer")
  }

  val units: MutableCollection<U>
  val unitClass: Class<U>

  fun disposeUnit(unit: U)

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

abstract class AbstractExplorerBase<U: WorkingSet<*>, UnitConfig: EntityWithUuid>: Explorer<U> {

  val lock = ReentrantReadWriteLock()

  val disposable = Disposer.newDisposable()

  abstract val unitConfigClass: Class<UnitConfig>

  abstract fun UnitConfig.toUnit(parentDisposable: Disposable): U

  override val componentManager: Application
    get() = ApplicationManager.getApplication()

  override var units: MutableSet<U> by lazyRwLocked(
    { configCrudable.getAll(unitConfigClass).map { it.toUnit(disposable) }.collect(Collectors.toSet()) },
    lock
  )


  override fun disposeUnit(unit: U) {
    configCrudable.getByUniqueKey(unitConfigClass, unit.uuid).let {
      configCrudable.delete(it)
    }
  }

  override fun isUnitPresented(unit: ExplorerUnit): Boolean {
    return unit.`is`(unitClass) && units.contains(unit)
  }


  override fun showNotification(title: String, content: String, type: NotificationType, project: Project?) {
    NotificationBuilder(
      EXPLORER_NOTIFICATION_GROUP_ID,
      title,
      content,
      type
    ).build().let {
      Notifications.Bus.notify(it, project)
    }
  }

  override fun reportThrowable(t: Throwable, project: Project?) {
    if (t is ProcessCanceledException) {
      return
    }
    NotificationBuilder(
      EXPLORER_NOTIFICATION_GROUP_ID,
      "Error in plugin For Mainframe",
      t.message ?: t.toString(),
      NotificationType.ERROR
    ).addAction(reportInSlackAction).build().let {
      Notifications.Bus.notify(it, project)
    }
  }

  private val reportInSlackAction = object : DumbAwareAction("Report In Slack") {
    override fun actionPerformed(e: AnActionEvent) {
      BrowserUtil.browse("https://join.slack.com/t/iba-mainframe-tools/shared_invite/zt-pejbtt4j-l7KuizvDedJSCxwGtxmMBg")
    }
  }

  override fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?) {
    reportThrowable(t, project)
  }

  fun doInit() {
    subscribe(CONFIGS_CHANGED, disposable, eventAdapter(unitConfigClass) {
      onDelete { unit ->
        lock.write {
          val found = units.find { it.uuid == unit.uuid } ?: return@onDelete
          Disposer.dispose(found)
          units.remove(found)
          sendTopic(UNITS_CHANGED).onDeleted(this@AbstractExplorerBase, found)
        }
      }
      onAdd { unit ->
        lock.write {
          val added = unit.toUnit(disposable)
          units.add(added)
          sendTopic(UNITS_CHANGED).onAdded(this@AbstractExplorerBase, added)
        }
      }
      onUpdate { _, unitConfig ->
        lock.read {
          val found = units.find { it.uuid == unitConfig.uuid }
          sendTopic(UNITS_CHANGED).onChanged(this@AbstractExplorerBase, found ?: return@onUpdate)
        }
      }
    })
    subscribe(CONFIGS_CHANGED, disposable, anyEventAdaptor<ConnectionConfig> {
      lock.read {
        units.forEach {
          sendTopic(UNITS_CHANGED).onChanged(this@AbstractExplorerBase, it)
        }
      }
    })
    subscribe(CREDENTIALS_CHANGED, disposable, CredentialsListener { uuid ->
      lock.read {
        val found = units.find { it.connectionConfig?.uuid == uuid }
        sendTopic(UNITS_CHANGED).onChanged(this@AbstractExplorerBase, found ?: return@CredentialsListener)
      }
    })
  }
}
