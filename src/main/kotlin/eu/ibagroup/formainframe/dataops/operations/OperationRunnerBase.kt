package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.FetchCallback

abstract class OperationRunnerBase<O : Operation, Prepared>(
  protected val dataOpsManager: DataOpsManager
) : OperationRunner<O> {

  protected abstract val tasksProvider: TasksProvider<O, Prepared, Unit, Unit>

  protected abstract fun tryToPrepare(operation: O): List<Prepared>

  override fun canRun(operation: O): Boolean {
    return tryToPrepare(operation).isNotEmpty()
  }

  override fun run(operation: O, callback: FetchCallback<Unit>, project: Project?) {
    val prepared = tryToPrepare(operation)
    if (prepared.isEmpty()) {
      throw IllegalArgumentException("Cannot run $operation using ${this::class.java.name}")
    }
    tasksProvider.runAsBackgroundable(operation, prepared, callback, project)
  }

}