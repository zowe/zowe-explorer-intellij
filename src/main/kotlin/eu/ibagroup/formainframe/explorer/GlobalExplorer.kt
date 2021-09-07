package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey

const val EXPLORER_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.ExplorerNotificationGroup"

class GlobalExplorerFactory : ExplorerFactory<FilesWorkingSet, GlobalExplorer> {
  override fun buildComponent(): GlobalExplorer = GlobalExplorer()
}

class GlobalExplorer : AbstractExplorerBase<FilesWorkingSet, FilesWorkingSetConfig>() {


  override fun FilesWorkingSetConfig.toUnit(parentDisposable: Disposable): GlobalWorkingSet {
    return GlobalWorkingSet(
      uuid = uuid,
      globalExplorer = this@GlobalExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  override val unitClass = FilesWorkingSet::class.java
  override val unitConfigClass = FilesWorkingSetConfig::class.java

  init {
    doInit()
  }

}
