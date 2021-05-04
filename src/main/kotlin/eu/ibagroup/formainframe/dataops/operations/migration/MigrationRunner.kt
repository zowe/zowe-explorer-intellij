package eu.ibagroup.formainframe.dataops.operations.migration

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.operations.*
import eu.ibagroup.formainframe.utils.UNIT_CLASS

@Suppress("UNCHECKED_CAST")
interface MigrationRunner<Q : Query<*, Unit>> : OperationRunner<Q, Unit> {

  override fun canRun(operation: Q): Boolean {
    return true
  }

  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS

}