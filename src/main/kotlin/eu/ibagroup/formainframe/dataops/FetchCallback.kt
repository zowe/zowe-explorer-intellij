package eu.ibagroup.formainframe.dataops

interface FetchCallback<R> {

  fun onSuccess(result: R)

  fun onThrowable(t: Throwable)

  fun onStart()

  fun onFinish()

}

class FetchAdapterBuilder<R> internal constructor() {
  private var onSuccess: (R) -> Unit = {}
  private var onThrowable: (Throwable) -> Unit = {}
  private var onStart: () -> Unit = {}
  private var onFinish: () -> Unit = {}
  private var onResult: (Result<R>) -> Unit = {  }

  fun onSuccess(callback: (R) -> Unit) {
    onSuccess = callback
  }

  fun onThrowable(callback: (Throwable) -> Unit) {
    onThrowable = callback
  }

  fun onStart(callback: () -> Unit) {
    onStart = callback
  }

  fun onFinish(callback: () -> Unit) {
    onFinish = callback
  }

  fun onResult(callback: (Result<R>) -> Unit) {
    onResult = callback
  }

  @PublishedApi
  internal val callback
    get() = object : FetchCallback<R> {
      override fun onSuccess(result: R) {
        this@FetchAdapterBuilder.onResult(Result.success(result))
        this@FetchAdapterBuilder.onSuccess(result)
      }

      override fun onThrowable(t: Throwable) {
        this@FetchAdapterBuilder.onResult(Result.failure(t))
        this@FetchAdapterBuilder.onThrowable(t)
      }

      override fun onStart() {
        this@FetchAdapterBuilder.onStart()
      }

      override fun onFinish() {
        this@FetchAdapterBuilder.onFinish()
      }
    }
}

fun <R> fetchAdapter(init: FetchAdapterBuilder<R>.() -> Unit): FetchCallback<R> {
  val adapterBuilder = FetchAdapterBuilder<R>().apply(init)
  return adapterBuilder.callback
}