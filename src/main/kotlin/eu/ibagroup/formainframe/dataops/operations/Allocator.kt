package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.utils.UNIT_CLASS

@Suppress("UNCHECKED_CAST")
interface Allocator<Q: Query<*, Unit>> : OperationRunner<Q, Unit> {

  override fun canRun(operation: Q): Boolean {
    return true
  }

  override val resultClass: Class<out Unit>
    get() = UNIT_CLASS

}