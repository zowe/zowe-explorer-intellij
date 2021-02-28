package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.FetchCallback
import java.io.IOException

interface OperationRunner<O : Operation> {

  companion object {
    @JvmStatic
    val EP = ExtensionPointName.create<OperationRunnerFactory>("eu.ibagroup.formainframe.operationRunner")
  }

  val operationClass: Class<out O>

  fun canRun(operation: O): Boolean

  fun run(operation: O, callback: FetchCallback<Unit>, project: Project? = null)

}