package eu.ibagroup.formainframe.dataops.api

import eu.ibagroup.formainframe.utils.lock
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class CallAdapter<E> @PublishedApi internal constructor() {
  @PublishedApi internal var onResponse: (Call<E>, Response<E>) -> Unit = { _, _ -> }
    private set
  @PublishedApi internal var onException: (Call<E>, Throwable) -> Unit = { _, _ -> }
    private set
  @PublishedApi internal var beforeAny: (Call<E>) -> Unit = {}
    private set
  @PublishedApi internal var afterAny: (Call<E>) -> Unit = {}
    private set

  fun onResponse(responseCallback: (call: Call<E>, response: Response<E>) -> Unit) {
    onResponse = responseCallback
  }

  fun onException(exceptionCallback: (call: Call<E>, t: Throwable) -> Unit) {
    onException = exceptionCallback
  }

  fun beforeAny(callback: (call: Call<E>) -> Unit) {
    beforeAny = callback
  }

  fun afterAny(callback: (call: Call<E>) -> Unit) {
    afterAny = callback
  }
}

inline fun <E> Call<E>.enqueue(lock: Lock? = null, condition: Condition? = null, init: CallAdapter<E>.() -> Unit) {
  assert((lock != null && condition != null) || (lock == null && condition == null))
  enqueue(CallAdapter<E>().apply(init).let {
    object : Callback<E> {
      override fun onResponse(call: Call<E>, response: Response<E>) {
        lock(lock) {
          it.beforeAny(call)
          it.onResponse(call, response)
          it.afterAny(call)
          condition?.signalAll()
        }
      }

      override fun onFailure(call: Call<E>, t: Throwable) {
        lock?.lock()
        it.beforeAny(call)
        it.onException(call, t)
        it.afterAny(call)
        condition?.signalAll()
        lock?.unlock()
      }
    }
  })
  lock(lock) { condition?.await() }
}

inline fun <E> Call<E>.enqueueSync(init: CallAdapter<E>.() -> Unit) {
  val lock = ReentrantLock()
  val condition = lock.newCondition()
  enqueue(lock, condition, init)
}