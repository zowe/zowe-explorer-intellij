package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator

interface TasksProvider<T, S, R> {

  fun combineResults(list: List<S>): R

  fun subtaskTitle(parameter: T): String

  fun run(parameter: T, progressIndicator: ProgressIndicator?): S

  val isCancellable: Boolean

}