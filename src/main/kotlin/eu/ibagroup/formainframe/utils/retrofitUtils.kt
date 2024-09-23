/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.TaskInfo
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Internal class to wrap the retrofit2 call */
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
    } catch (e: Throwable) {
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

/**
 * Set up the call cancellation on the progress indicator finish
 * @param progressIndicator the progress indicator to cancel by
 */
inline fun <reified T : Any> Call<T>.cancelByIndicator(progressIndicator: ProgressIndicator): Call<T> {
  val call = this
  return if (progressIndicator is ProgressIndicatorEx) {
    val wrapped = WrappedCancellableCall(this)
    val delegate = object : AbstractProgressIndicatorBase(), ProgressIndicatorEx {
      override fun cancel() {
        call.cancel()
        super.cancel()
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
