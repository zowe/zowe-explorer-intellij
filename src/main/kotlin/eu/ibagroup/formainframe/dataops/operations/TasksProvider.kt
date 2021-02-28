package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.FetchCallback
import java.util.concurrent.atomic.AtomicInteger
import kotlin.streams.toList

interface TasksProvider<O: Operation, T, S, R> {

  fun makeTaskTitle(operation: O): String

  fun combineResults(list: List<S>): R

  fun subtaskTitle(parameter: T): String

  fun run(parameter: T, progressIndicator: ProgressIndicator): S

  val isCancellable: Boolean

}

fun <O: Operation, T, S, R> TasksProvider<O, T, S, R>.runAsBackgroundable(
  operation: O,
  parameters: List<T>,
  callback: FetchCallback<R>,
  project: Project? = null
) {
  ProgressManager.getInstance().run(object : Task.Backgroundable(project, makeTaskTitle(operation), isCancellable) {
    override fun run(indicator: ProgressIndicator) {
      callback.onStart()
      try {
        val total = parameters.size
        val counter = AtomicInteger(0)
        val result = parameters.parallelStream().map {
          indicator.text = this@runAsBackgroundable.subtaskTitle(it)
          this@runAsBackgroundable.run(it, indicator).also {
            indicator.fraction = counter.incrementAndGet().toDouble() / total
          }
        }.toList().let { this@runAsBackgroundable.combineResults(it) }
        callback.onSuccess(result)
      } catch (t: Throwable) {
        callback.onThrowable(t)
      } finally {
        callback.onFinish()
      }
    }
  })
}