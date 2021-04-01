package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CREDENTIALS_CHANGED
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialsListener
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.*
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.anyEventAdaptor
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import kotlin.concurrent.read

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

  override val units: MutableSet<GlobalWorkingSet> = lock(lock.writeLock()) {
    configCrudable.getAll<WorkingSetConfig>().map { it.toGlobalWs(disposable) }.collect(Collectors.toSet())
  }
    get() = lock(lock.readLock()) { field }

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
    Messages.showErrorDialog(project, t.message ?: t.toString(), "Error")
  }

  override fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?) {
    reportThrowable(t, project)
  }

  init {
    subscribe(ConfigService.CONFIGS_CHANGED, eventAdaptor<WorkingSetConfig> {
      onDelete { ws ->
        lock(lock.writeLock()) {
          val found = units.find { it.uuid == ws.uuid } ?: return@onDelete
          Disposer.dispose(found)
          units.remove(found)
          sendTopic(Explorer.UNITS_CHANGED).onDeleted(this@GlobalExplorer, found)
        }
      }
      onAdd { ws ->
        lock(lock.writeLock()) {
          val added = ws.toGlobalWs(disposable)
          units.add(added)
          sendTopic(Explorer.UNITS_CHANGED).onAdded(this@GlobalExplorer, added)
        }
      }
      onUpdate { _, workingSetConfig ->
        lock.read {
          val found = units.find { it.uuid == workingSetConfig.uuid }
          sendTopic(Explorer.UNITS_CHANGED).onChanged(this@GlobalExplorer, found ?: return@onUpdate)
        }
      }
    }, disposable)
    subscribe(ConfigService.CONFIGS_CHANGED, anyEventAdaptor<ConnectionConfig> { connectionConfig ->
      lock.read {
        val found = units.find { it.connectionConfig?.uuid == connectionConfig.uuid }
        sendTopic(Explorer.UNITS_CHANGED).onChanged(this@GlobalExplorer, found ?: return@anyEventAdaptor)
      }
    }, disposable)
    subscribe(ConfigService.CONFIGS_CHANGED, anyEventAdaptor<UrlConnection> { urlConnection ->
      lock.read {
        val found = units.find { it.connectionConfig?.urlConnectionUuid == urlConnection.uuid }
        sendTopic(Explorer.UNITS_CHANGED).onChanged(this@GlobalExplorer, found ?: return@anyEventAdaptor)
      }
    }, disposable)
    subscribe(CREDENTIALS_CHANGED, CredentialsListener { uuid ->
      lock.read {
        val found = units.find { it.connectionConfig?.uuid == uuid }
        sendTopic(Explorer.UNITS_CHANGED).onChanged(this@GlobalExplorer, found ?: return@CredentialsListener)
      }
    }, disposable)
  }

}