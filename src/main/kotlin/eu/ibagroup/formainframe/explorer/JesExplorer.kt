/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.rwLocked
import java.util.stream.Collectors


class JesExplorerFactory : ExplorerFactory<JesWorkingSet, JesExplorer> {
  override fun buildComponent(): JesExplorer = JesExplorer()
}

class JesExplorer : AbstractExplorerBase<JesWorkingSet, JobsWorkingSetConfig>() {
  override val unitClass = JesWorkingSet::class.java
  override val unitConfigClass = JobsWorkingSetConfig::class.java

  override val units by rwLocked(
    configCrudable.getAll(unitConfigClass).map { it.toUnit(disposable) }.collect(Collectors.toSet()).toMutableSet(),
    lock
  )

  override fun JobsWorkingSetConfig.toUnit(parentDisposable: Disposable): JesWorkingSet {
    return GlobalJesWorkingSet(
      uuid = uuid,
      globalExplorer = this@JesExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }
  init {
    doInit()
  }

}

class JobsWorkingSetUnit(
  override val explorer: Explorer<*>,
  val jobsWorkingSetConfig: JobsWorkingSetConfig
) : ExplorerUnit {
  override val name = jobsWorkingSetConfig.name
  override val uuid = jobsWorkingSetConfig.uuid
  override val connectionConfig: ConnectionConfig? = configCrudable.getByUniqueKey(jobsWorkingSetConfig.connectionConfigUuid)
}

class JesFilterUnit(
  override val explorer: Explorer<*>,
  val jobsFilter: JobsFilter
) : ExplorerUnit {
  override val name: String = jobsFilter.toString()
  override val uuid: String = ""
  override val connectionConfig: ConnectionConfig? = configCrudable.getByUniqueKey(""/*jobsFilter.connectionConfigUuid*/)
}
