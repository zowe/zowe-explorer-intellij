package eu.ibagroup.formainframe.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.Invoker
import kotlinx.coroutines.*
import org.jetbrains.concurrency.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias Computation<R> = (progressIndicator: ProgressIndicator) -> R

typealias PromiseInvoker<R> = (Computation<R>) -> Promise<R>

abstract class TaskPromiseInvoker<R> : PromiseInvoker<R> {

  var indicator: ProgressIndicator? = null
    protected set

}

fun <R> Invoker.asPromiseInvoker(progressIndicator: ProgressIndicator, later: Boolean = false): PromiseInvoker<R> {
  return object : PromiseInvoker<R> {
    override fun invoke(computation: Computation<R>): Promise<R> {
      return if (later) {
        this@asPromiseInvoker.computeLater { computation(progressIndicator) }
      } else {
        this@asPromiseInvoker.compute { computation(progressIndicator) }
      }
    }
  }
}

class BackgroundTaskPromiseInvoker<R>(
  private val title: String,
  private val project: Project? = null,
  private val canBeCancelled: Boolean = true,
  private val cancelPromise: Boolean = false,
) : TaskPromiseInvoker<R>() {

  private lateinit var promise: Promise<R>

  override fun invoke(computation: Computation<R>): Promise<R> {
    val promiseLock = ReentrantLock()
    val promiseCondition = promiseLock.newCondition()
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, canBeCancelled) {

      override fun run(indicator: ProgressIndicator) {
        this@BackgroundTaskPromiseInvoker.indicator = indicator
        val executionLock = ReentrantLock()
        val executionCondition = executionLock.newCondition()
        promise = runAsync {
          computation(indicator).also {
            executionLock.withLock { executionCondition.signalAll() }
          }
        }
        promiseLock.withLock { promiseCondition.signalAll() }
        executionLock.withLock {
          executionCondition.await()
        }
      }

      override fun onCancel() {
        if (cancelPromise) {
          promise.castOrNull<CancellablePromise<*>>()?.cancel()
        }
      }
    })
    while (!this::promise.isInitialized) {
      promiseLock.withLock { promiseCondition.await() }
    }
    return promise
  }
}

