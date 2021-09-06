package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getByForeignKey
import eu.ibagroup.formainframe.utils.runIfTrue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class GlobalJesWorkingSet(
  override val uuid: String,
  globalExplorer: JesExplorer,
  private val workingSetConfigProvider: (String) -> JobsWorkingSetConfig?,
  parentDisposable: Disposable
): JesWorkingSet {


  private val lock = ReentrantLock()

  private val isDisposed = AtomicBoolean(false)

  private val workingSetConfig: JobsWorkingSetConfig?
    get() = lock.withLock {
      (isDisposed.compareAndSet(false, false)).runIfTrue { workingSetConfigProvider(uuid) }
    }

  init {
    Disposer.register(parentDisposable, this)
  }

  override val masks: Collection<JobsFilter>
    get() = lock.withLock { workingSetConfig?.jobsFilters ?: listOf() }

  override fun addMask(mask: JobsFilter) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.jobsFilters.add(mask)) {
      configCrudable.update(newWsConfig)
    }
  }

  override fun removeMask(mask: JobsFilter) {
    val newWsConfig = workingSetConfig?.clone() ?: return
    if (newWsConfig.jobsFilters.remove(mask)) {
      configCrudable.update(newWsConfig)
    }
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

  override val explorer = globalExplorer

  override fun dispose() {
    isDisposed.set(true)
  }
}
