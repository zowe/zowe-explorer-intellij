package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.util.stream.Collectors

const val EXPLORER_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.ExplorerNotificationGroup"

class GlobalExplorerFactory : ExplorerFactory<GlobalWorkingSet, GlobalExplorer> {
  override fun buildComponent(): GlobalExplorer = GlobalExplorer()
}

class GlobalExplorer : AbstractExplorerBase<GlobalWorkingSet, FilesWorkingSetConfig>() {


  override fun FilesWorkingSetConfig.toUnit(parentDisposable: Disposable): GlobalWorkingSet {
    return GlobalWorkingSet(
      uuid = uuid,
      globalExplorer = this@GlobalExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  override val unitClass = GlobalWorkingSet::class.java
  override val unitConfigClass = FilesWorkingSetConfig::class.java

  override val units by rwLocked(
    configCrudable.getAll(unitConfigClass).map { it.toUnit(disposable) }.collect(Collectors.toSet()).toMutableSet(),
    lock
  )

  init {
    doInit()
  }



}
