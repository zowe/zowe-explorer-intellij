package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.utils.clone
import kotlin.concurrent.withLock

class GlobalWorkingSet(
  override val uuid: String,
  globalExplorer: AbstractExplorerBase<GlobalWorkingSet, FilesWorkingSetConfig>,
  private val workingSetConfigProvider: (String) -> FilesWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<DSMask, WorkingSet<*>, FilesWorkingSetConfig>(
  uuid,
  globalExplorer,
  workingSetConfigProvider,
  parentDisposable
), FilesWorkingSet {

  override val wsConfigClass = FilesWorkingSetConfig::class.java

  override fun FilesWorkingSetConfig.masks() = this.dsMasks

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

  init {
    Disposer.register(parentDisposable, this)
  }
}
