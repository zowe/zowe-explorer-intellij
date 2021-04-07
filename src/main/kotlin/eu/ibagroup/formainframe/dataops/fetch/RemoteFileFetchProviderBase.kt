package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.runIfTrue
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.formainframe.utils.sendTopic
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.Collection
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.none
import kotlin.collections.set
import kotlin.concurrent.withLock
import kotlin.streams.toList

abstract class RemoteFileFetchProviderBase<Request : Any, Response : Any, File : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : FileFetchProvider<Request, RemoteQuery<Request, Unit>, File> {

  private enum class CacheState {
    FETCHED, ERROR
  }

  private val lock = ReentrantLock()

  private val cache = mutableMapOf<RemoteQuery<Request, Unit>, Collection<File>>()
  private val cacheState = mutableMapOf<RemoteQuery<Request, Unit>, CacheState>()

  override fun getCached(query: RemoteQuery<Request, Unit>): Collection<File>? {
    return lock(lock) { (cacheState[query] == CacheState.FETCHED).runIfTrue { cache[query] } }
  }

  override fun isCacheValid(query: RemoteQuery<Request, Unit>): Boolean {
    return lock.withLock { cacheState[query] != CacheState.ERROR }
  }

  protected abstract fun fetchResponse(query: RemoteQuery<Request, Unit>): Collection<Response>

  protected abstract fun convertResponseToFile(response: Response): File

  protected abstract fun cleanupUnusedFile(file: File, query: RemoteQuery<Request, Unit>)

  open fun compareOldAndNewFile(oldFile: File, newFile: File): Boolean {
    return oldFile.path == newFile.path
  }

  override fun reload(query: RemoteQuery<Request, Unit>, progressIndicator: ProgressIndicator?) {
    runCatching {
      val fetched = fetchResponse(query)
      if (progressIndicator?.isCanceled == true) {
        return@runCatching getCached(query) ?: listOf()
      }
      val files = runWriteActionOnWriteThread {
        fetched.map {
          convertResponseToFile(it)
        }
      }

      cache[query]?.parallelStream()?.filter { oldFile ->
        oldFile.isValid && files.none { compareOldAndNewFile(oldFile, it) }
      }?.toList()?.apply {
        runWriteActionOnWriteThread {
          forEach { cleanupUnusedFile(it, query) }
        }
      }
      cache[query] = files
      cacheState[query] = CacheState.FETCHED
      files
    }.onSuccess {
      sendTopic(FileFetchProvider.CACHE_UPDATED, dataOpsManager.componentManager).onCacheUpdated(query, it)
    }.onFailure {
      cache[query] = listOf()
      cacheState[query] = CacheState.ERROR
      sendTopic(FileFetchProvider.CACHE_UPDATED, dataOpsManager.componentManager).onFetchFailure(query, it)
    }
  }

  override fun cleanCache(query: RemoteQuery<Request, Unit>) {
    cache.remove(query)
    cacheState.remove(query)
    sendTopic(FileFetchProvider.CACHE_UPDATED, dataOpsManager.componentManager).onCacheUpdated(query, listOf())
  }

  abstract val responseClass: Class<out Response>

  override val queryClass = RemoteQuery::class.java
}