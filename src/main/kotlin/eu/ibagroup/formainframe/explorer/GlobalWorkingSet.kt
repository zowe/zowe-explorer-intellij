package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.crudable.getByForeignKey
import eu.ibagroup.formainframe.utils.crudable.getByForeignKeyDeeply
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.subscribe
import java.util.concurrent.locks.ReentrantReadWriteLock

class GlobalWorkingSet(
  globalExplorer: GlobalExplorer,
  workingSetConfig: WorkingSetConfig,
  parentDisposable: Disposable
) : WorkingSet, Disposable {

  override val explorer = globalExplorer

  private val lock = ReentrantReadWriteLock()

  private var workingSetConfig: WorkingSetConfig? = workingSetConfig
    get() = lock(lock.readLock()) { field }
    set(value) = lock(lock.writeLock()) { field = value }

  init {
    Disposer.register(parentDisposable, this)
    subscribe(ConfigService.CONFIGS_CHANGED, eventAdaptor<WorkingSetConfig> {
      onUpdate { old, new -> if (old.uuid == uuid) this@GlobalWorkingSet.workingSetConfig = new }
      onDelete { dispose() }
    }, this)
  }

  override val name = workingSetConfig.name

  override val uuid = workingSetConfig.uuid

  override val connectionConfig: ConnectionConfig?
    get() = lock(lock.readLock()) { workingSetConfig?.let { configCrudable.getByForeignKey(it) } }

  override val urlConnection: UrlConnection?
    get() = lock(lock.readLock()) { workingSetConfig?.let { configCrudable.getByForeignKeyDeeply(it) } }

  override val dsMasks: Collection<DSMask>
    get() = lock(lock.readLock()) { workingSetConfig?.dsMasks ?: listOf() }

  override val ussPaths: Collection<UssPath>
    get() = lock(lock.readLock()) { workingSetConfig?.ussPaths ?: listOf() }

  override fun onThrowable(t: Throwable) {
    println(t.message)
  }

  override fun dispose() {
    workingSetConfig = null
  }

}