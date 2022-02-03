package eu.ibagroup.formainframe.config.ws.ui.files

import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.ui.*
import eu.ibagroup.formainframe.utils.crudable.*

class WSTableModel(crudable: Crudable) : AbstractWsTableModel<FilesWorkingSetConfig>(crudable) {

  override val clazz = FilesWorkingSetConfig::class.java

  override operator fun set(row: Int, item: FilesWorkingSetConfig) {
    get(row).dsMasks = item.dsMasks
    get(row).ussPaths = item.ussPaths
    super.set(row, item)
  }

  init {
    initialize()
  }
}
