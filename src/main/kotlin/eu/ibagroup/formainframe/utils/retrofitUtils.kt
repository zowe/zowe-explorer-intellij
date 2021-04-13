package eu.ibagroup.formainframe.utils

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.TaskInfo
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

@PublishedApi
internal class WrappedCancellableCall<T>(private val call: Call<T>) : Call<T> by call {
  override fun clone() = WrappedCancellableCall(call.clone())

  private fun buildException(t: Throwable): Throwable {
    return if (call.isCanceled) {
      ProcessCanceledException(t)
    } else {
      t
    }
  }

  override fun execute(): Response<T> {
    return try {
      call.execute()
    } catch (e : Throwable) {
      throw buildException(e)
    }
  }

  override fun enqueue(callback: Callback<T>) {
    call.enqueue(object : Callback<T> {
      override fun onResponse(call: Call<T>, response: Response<T>) {
        callback.onResponse(call, response)
      }

      override fun onFailure(call: Call<T>, t: Throwable) {
        callback.onFailure(call, buildException(t))
      }
    })
  }
}

inline fun <reified T : Any> Call<T>.cancelByIndicator(progressIndicator: ProgressIndicator): Call<T> {
  return if (progressIndicator is ProgressIndicatorEx) {
    val wrapped = WrappedCancellableCall(this)
    val delegate = object : AbstractProgressIndicatorBase(), ProgressIndicatorEx {
      override fun cancel() {
        super.cancel()
        this@cancelByIndicator.cancel()
      }

      override fun addStateDelegate(delegate: ProgressIndicatorEx) {
        throw UnsupportedOperationException()
      }

      override fun finish(task: TaskInfo) {
      }

      override fun isFinished(task: TaskInfo) = true

      override fun wasStarted() = false

      override fun processFinish() {
      }
    }
    progressIndicator.addStateDelegate(delegate)
    wrapped
  } else {
    this
  }
}