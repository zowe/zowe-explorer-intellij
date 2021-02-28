package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery

abstract class RemoteAllocatorBase<Q : RemoteQuery<*, Unit>>(private val dataOpsManager: DataOpsManager) :
  Allocator<Q> {

  override fun run(
    operation: Q,
    progressIndicator: ProgressIndicator?
  ) {
    performAllocationRequest(operation,progressIndicator)
  }

  abstract fun performAllocationRequest(query: Q, progressIndicator: ProgressIndicator?)

}