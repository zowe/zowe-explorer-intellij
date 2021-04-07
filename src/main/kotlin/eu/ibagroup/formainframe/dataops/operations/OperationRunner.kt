package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.dataops.Operation

interface OperationRunner<O : Operation<R>, R : Any> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<OperationRunnerFactory>("eu.ibagroup.formainframe.operationRunner")
  }

  val operationClass: Class<out O>

  val resultClass: Class<out R>

  fun canRun(operation: O): Boolean

  fun run(operation: O, progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE): R

}