package eu.ibagroup.formainframe.explorer

import com.intellij.notification.NotificationBuilder
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.CONFIGS_CHANGED
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CREDENTIALS_CHANGED
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialsListener
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.anyEventAdaptor
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import kotlin.concurrent.read
import kotlin.concurrent.write

const val EXPLORER_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.ExplorerNotificationGroup"

class GlobalExplorer : Explorer {

  private fun WorkingSetConfig.toGlobalWs(parentDisposable: Disposable): GlobalWorkingSet {
    return GlobalWorkingSet(
      uuid = uuid,
      globalExplorer = this@GlobalExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  val disposable = Disposer.newDisposable()

  val lock = ReentrantReadWriteLock()

  override val units: MutableSet<GlobalWorkingSet> by rwLocked(
    value = configCrudable.getAll<WorkingSetConfig>().map { it.toGlobalWs(disposable) }.collect(Collectors.toSet()),
    lock = lock
  )

  override fun disposeUnit(unit: ExplorerUnit) {
    configCrudable.getByUniqueKey<WorkingSetConfig>(unit.uuid)?.let {
      configCrudable.delete(it)
    }
  }

  override fun isUnitPresented(unit: ExplorerUnit): Boolean {
    return unit is GlobalWorkingSet && units.contains(unit)
  }

  override val componentManager: Application
    get() = ApplicationManager.getApplication()

  override fun reportThrowable(t: Throwable, project: Project?) {
    if (t is ProcessCanceledException) {
      return
    }
    val details = t.castOrNull<CallException>()?.details
    NotificationBuilder(
      EXPLORER_NOTIFICATION_GROUP_ID,
      "Error in plugin For Mainframe",
      t.message ?: t.toString(),
      NotificationType.ERROR
    ).applyIfNotNull(details) {
      setDropDownText(it)
//      setContextHelpAction(object : DumbAwareAction("Show Info") {
//        override fun actionPerformed(e: AnActionEvent) {
//          runInEdt {
//            Messages.showInfoMessage(it, t.message ?: t.toString())
//          }
//        }
//      })
    }.build().let {
      Notifications.Bus.notify(it, project)
    }
  }

  override fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?) {
    reportThrowable(t, project)
  }

  init {
    subscribe(CONFIGS_CHANGED, disposable, eventAdaptor<WorkingSetConfig> {
      onDelete { ws ->
        lock.write {
          val found = units.find { it.uuid == ws.uuid } ?: return@onDelete
          Disposer.dispose(found)
          units.remove(found)
          sendTopic(UNITS_CHANGED).onDeleted(this@GlobalExplorer, found)
        }
      }
      onAdd { ws ->
        lock.write {
          val added = ws.toGlobalWs(disposable)
          units.add(added)
          sendTopic(UNITS_CHANGED).onAdded(this@GlobalExplorer, added)
        }
      }
      onUpdate { _, workingSetConfig ->
        lock.read {
          val found = units.find { it.uuid == workingSetConfig.uuid }
          sendTopic(UNITS_CHANGED).onChanged(this@GlobalExplorer, found ?: return@onUpdate)
        }
      }
    })
    subscribe(CONFIGS_CHANGED, disposable, anyEventAdaptor<ConnectionConfig> {
      lock.read {
        units.forEach {
          sendTopic(UNITS_CHANGED).onChanged(this@GlobalExplorer, it)
        }
      }
    })
    subscribe(CONFIGS_CHANGED, disposable, anyEventAdaptor<UrlConnection> {
      lock.read {
        units.forEach {
          sendTopic(UNITS_CHANGED).onChanged(this@GlobalExplorer, it)
        }
      }
    })
    subscribe(CREDENTIALS_CHANGED, disposable, CredentialsListener { uuid ->
      lock.read {
        val found = units.find { it.connectionConfig?.uuid == uuid }
        sendTopic(UNITS_CHANGED).onChanged(this@GlobalExplorer, found ?: return@CredentialsListener)
      }
    })
  }

}