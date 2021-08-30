/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.CONFIGS_CHANGED
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CREDENTIALS_CHANGED
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialsListener
import eu.ibagroup.formainframe.config.jobs.JobsFilter
import eu.ibagroup.formainframe.utils.crudable.anyEventAdaptor
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.utils.subscribe
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import kotlin.concurrent.read
import kotlin.concurrent.write


class JesExplorerFactory : ExplorerFactory<JesExplorer> {
  override fun buildComponent(): JesExplorer = JesExplorer()
}

class JesExplorer : Explorer {

  override val units: MutableList<JesFilterUnit> = configCrudable.getAll<JobsFilter>().map {
    JesFilterUnit(this, it)
  }.collect(Collectors.toList())

  override fun disposeUnit(unit: ExplorerUnit) {

  }

  val disposable = Disposer.newDisposable()

  val lock = ReentrantReadWriteLock()

  override fun isUnitPresented(unit: ExplorerUnit): Boolean = units.contains(unit)

  override val componentManager: ComponentManager
    get() = ApplicationManager.getApplication()

  init {
    subscribe(CONFIGS_CHANGED, disposable, eventAdaptor<JobsFilter> {
      onDelete { jf ->
        lock.write {
          val found = units.find { it.uuid == jf.uuid } ?: return@onDelete
          //Disposer.dispose(found)
          units.remove(found)
          sendTopic(UNITS_CHANGED).onDeleted(this@JesExplorer, found)
        }
      }
      onAdd { jf ->
        lock.write {
          val added = JesFilterUnit(this@JesExplorer, jf)
          units.add(added)
          sendTopic(UNITS_CHANGED).onAdded(this@JesExplorer, added)
        }
      }
      onUpdate { _, workingSetConfig ->
        lock.read {
          val found = units.find { it.uuid == workingSetConfig.uuid }
          sendTopic(UNITS_CHANGED).onChanged(this@JesExplorer, found ?: return@onUpdate)
        }
      }
    })
    subscribe(CONFIGS_CHANGED, disposable, anyEventAdaptor<ConnectionConfig> {
      lock.read {
        units.forEach {
          sendTopic(UNITS_CHANGED).onChanged(this@JesExplorer, it)
        }
      }
    })
    subscribe(CREDENTIALS_CHANGED, disposable, CredentialsListener { uuid ->
      lock.read {
        val found = units.find { it.connectionConfig?.uuid == uuid }
        sendTopic(UNITS_CHANGED).onChanged(this@JesExplorer, found ?: return@CredentialsListener)
      }
    })
  }


  override fun reportThrowable(t: Throwable, project: Project?) {

  }

  override fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?) {
    TODO("Not yet implemented")
  }


  override fun showNotification(title: String, content: String, type: NotificationType, project: Project?) {

  }
}

class JesFilterUnit(
  override val explorer: Explorer,
  val jobsFilter: JobsFilter
) : ExplorerUnit {
  override val name: String = jobsFilter.toString()
  override val uuid: String = jobsFilter.uuid
  override val connectionConfig: ConnectionConfig? = configCrudable.getByUniqueKey(jobsFilter.connectionConfigUuid)

}