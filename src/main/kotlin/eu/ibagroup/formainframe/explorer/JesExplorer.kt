/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.rwLocked
import java.util.stream.Collectors


class JesExplorerFactory : ExplorerFactory<GlobalJesWorkingSet, JesExplorer> {
  override fun buildComponent(): JesExplorer = JesExplorer()
}

class JesExplorer() : AbstractExplorerBase<GlobalJesWorkingSet, JobsWorkingSetConfig>() {
  override val unitClass = GlobalJesWorkingSet::class.java
  override val unitConfigClass = JobsWorkingSetConfig::class.java

  override val units by rwLocked(
    configCrudable.getAll(unitConfigClass).map { it.toUnit(disposable) }.collect(Collectors.toSet()).toMutableSet(),
    lock
  )

  override fun JobsWorkingSetConfig.toUnit(parentDisposable: Disposable): GlobalJesWorkingSet {
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
