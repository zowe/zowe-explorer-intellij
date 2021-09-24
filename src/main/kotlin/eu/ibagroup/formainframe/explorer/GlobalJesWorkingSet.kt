package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig

class GlobalJesWorkingSet(
  override val uuid: String,
  globalExplorer: AbstractExplorerBase<GlobalJesWorkingSet, JobsWorkingSetConfig>,
  workingSetConfigProvider: (String) -> JobsWorkingSetConfig?,
  parentDisposable: Disposable
) : WorkingSetBase<JobsFilter, WorkingSet<*>, JobsWorkingSetConfig>(
  uuid,
  globalExplorer,
  workingSetConfigProvider,
  parentDisposable
) {
  override val wsConfigClass = JobsWorkingSetConfig::class.java

  override fun JobsWorkingSetConfig.masks(): MutableCollection<JobsFilter> = this.jobsFilters

  init {
    Disposer.register(parentDisposable, this)
  }
}
