package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByForeignKey
import eu.ibagroup.formainframe.utils.crudable.getByForeignKeyDeeply
import eu.ibagroup.formainframe.utils.runIfTrue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock

class GlobalWorkingSet(
  override val uuid: String,
  globalExplorer: GlobalExplorer,
  private val workingSetConfigProvider: (String) -> WorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSet, Disposable {

  override val explorer = globalExplorer

  private val lock = ReentrantLock()

  private val isDisposed = AtomicBoolean(false)

  private val workingSetConfig: WorkingSetConfig?
    get() = lock.withLock {
      (isDisposed.compareAndSet(false, false)).runIfTrue { workingSetConfigProvider(uuid) }
    }

  init {
    Disposer.register(parentDisposable, this)
  }

  override val name
    get() = workingSetConfig?.name ?: ""

  override val connectionConfig: ConnectionConfig?
    get() = lock.withLock {
      workingSetConfig
        ?.let {
          return@withLock configCrudable.getByForeignKey(it)
        }
    }

  override val urlConnection: UrlConnection?
    get() = lock.withLock { workingSetConfig?.let { configCrudable.getByForeignKeyDeeply(it) } }

  override val dsMasks: Collection<DSMask>
    get() = lock.withLock { workingSetConfig?.dsMasks ?: listOf() }

  override fun addMask(dsMask: DSMask) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.dsMasks.add(dsMask)) {
      configCrudable.update(newWsConfig)
    }
  }

  override fun removeMask(dsMask: DSMask) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.dsMasks.remove(dsMask)) {
      configCrudable.update(newWsConfig)
    }
  }

  override val ussPaths: Collection<UssPath>
    get() = lock.withLock { workingSetConfig?.ussPaths ?: listOf() }

  override fun addUssPath(ussPath: UssPath) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.ussPaths.add(ussPath)) {
      configCrudable.update(newWsConfig)
    }
  }

  override fun removeUssPath(ussPath: UssPath) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.ussPaths.remove(ussPath)) {
      configCrudable.update(newWsConfig)
    }
  }

  override fun dispose() {
    isDisposed.set(true)
  }

}