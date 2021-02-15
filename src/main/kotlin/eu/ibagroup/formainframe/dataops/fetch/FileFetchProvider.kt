package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.utils.sendTopic

fun sendCacheUpdatedTopic(): FileCacheListener = sendTopic(FileFetchProvider.CACHE_UPDATED)

interface FetchCallback<File : VirtualFile> {

  fun onSuccess(files: Collection<File>)

  fun onThrowable(t: Throwable)

  fun onStart()

  fun onFinish()

}

class FetchAdapterBuilder<File : VirtualFile> internal constructor() {
  private var onSuccess: (Collection<File>) -> Unit = {}
  private var onThrowable: (Throwable) -> Unit = {}
  private var onStart: () -> Unit = {}
  private var onFinish: () -> Unit = {}
  fun onSuccess(callback: (Collection<File>) -> Unit) {
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
  @PublishedApi internal val callback
    get() = object : FetchCallback<File> {
      override fun onSuccess(files: Collection<File>) {
        this@FetchAdapterBuilder.onSuccess(files)
      }

      override fun onThrowable(t: Throwable) {
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

fun <File : VirtualFile> fetchAdapter(init: FetchAdapterBuilder<File>.() -> Unit): FetchCallback<File> {
  val adapterBuilder = FetchAdapterBuilder<File>().apply(init)
  return adapterBuilder.callback
}

fun <File : VirtualFile> emptyCallback() = fetchAdapter<File> {  }

interface FileFetchProvider<R : Any, Q : Query<R>, File : VirtualFile> {

  companion object {
    @JvmStatic
    val CACHE_UPDATED = Topic.create("cacheUpdated", FileCacheListener::class.java)
  }

  fun getCached(query: Q): Collection<File>?

  fun forceReloadSynchronous(query: Q): Collection<File>

  fun getCachedOrReloadSynchronous(query: Q): Collection<File> {
    return getCached(query) ?: forceReloadSynchronous(query)
  }

  fun getCachedOrReloadAsync(query: Q, callback: FetchCallback<File> = emptyCallback()) {
    val cached = getCached(query)
    if (cached != null) {
      callback.onSuccess(cached)
    } else {
      forceReloadAsync(query, callback)
    }
  }

  fun forceReloadAsync(query: Q, callback: FetchCallback<File> = emptyCallback())

  val requestClass: Class<out R>

  val queryClass: Class<out Query<*>>

  val vFileClass: Class<out File>

}