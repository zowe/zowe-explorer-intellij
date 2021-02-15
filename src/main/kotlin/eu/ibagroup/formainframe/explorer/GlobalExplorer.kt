package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.*
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.indexOf
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.formainframe.utils.toMutableList
import java.util.concurrent.locks.ReentrantReadWriteLock

class GlobalExplorer : Explorer {

  private fun WorkingSetConfig.toGlobalWs(parentDisposable: Disposable): GlobalWorkingSet {
    return GlobalWorkingSet(this@GlobalExplorer, this, parentDisposable)
  }

  val disposable = Disposer.newDisposable()

  val lock = ReentrantReadWriteLock()

  override val units = lock(lock.writeLock()) {
    configCrudable.getAll<WorkingSetConfig>().map { it.toGlobalWs(disposable) }.toMutableList()
  }
    get() = lock(lock.readLock()) { field }

  init {
    subscribe(ConfigService.CONFIGS_CHANGED, eventAdaptor<WorkingSetConfig> {
      onDelete { ws ->
        lock(lock.writeLock()) { units.removeIf { it.uuid == ws.uuid } }
      }
      onAdd { ws ->
        lock(lock.writeLock()) { units.add(ws.toGlobalWs(disposable))/*.also { it.forceAsyncUpdate() })*/ }
      }
      onUpdate { oldConfig, newConfig ->
        lock(lock.writeLock()) {
          units.indexOf { it.uuid == oldConfig.uuid }?.let { index ->
            units[index] = newConfig.toGlobalWs(disposable)/*.also { it.forceAsyncUpdate()*/
          }
        }
      }
    })
    //units.filterIsInstance<WorkingSet>().forceAsyncUpdate()
  }
//
//  private fun WorkingSet.forceAsyncUpdate() {
//    listOf(this).forceAsyncUpdate()
//  }

//  private fun List<WorkingSet>.forceAsyncUpdate() {
//    map { ws ->
//      ws.dsMasks.mapNotNull {
//        val connectionConfig = ws.connectionConfig
//        val urlConnection = ws.urlConnection
//        if (connectionConfig != null && urlConnection != null) {
//          RemoteQueryImpl(it, connectionConfig, urlConnection)
//        } else null
//      }
//    }.flatten().parallelStream().forEach { getRemoteMfFileFetchProvider<DSMask>().forceReloadAsync(it) }
//  }

  override val project: Project? = null

}