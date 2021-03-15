package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.Invoker
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.runIfTrue
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.formainframe.utils.sendTopic
import org.jetbrains.concurrency.Promise
import java.util.concurrent.locks.ReentrantLock
import kotlin.streams.toList

abstract class RemoteFileFetchProviderBase<Request : Any, Response : Any, File : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : FileFetchProvider<Request, RemoteQuery<Request, Unit>, File> {

  private val lock = ReentrantLock()

  private val cache = mutableMapOf<RemoteQuery<Request, Unit>, Collection<File>>()
  private val cacheState = mutableMapOf<RemoteQuery<Request, Unit>, Boolean>()

  override fun getCached(query: RemoteQuery<Request, Unit>): Collection<File>? {
    return lock(lock) { cacheState[query].runIfTrue { cache[query] } }
  }

  protected abstract fun fetchResponse(query: RemoteQuery<Request, Unit>): Collection<Response>

  protected abstract fun convertResponseToFile(response: Response): File

  protected abstract fun cleanupUnusedFile(file: File, query: RemoteQuery<Request, Unit>)

  open fun compareOldAndNewFile(oldFile: File, newFile: File): Boolean {
    return oldFile.path == newFile.path
  }

  override fun forceReload(query: RemoteQuery<Request, Unit>): Promise<Collection<File>> {
    @Suppress("UNCHECKED_CAST")
    return Invoker.forBackgroundThreadWithoutReadAction(dataOpsManager)
      .compute {
        val fetched = fetchResponse(query)
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
        cacheState[query] = true
        files
      }.onSuccess {
        sendTopic(FileFetchProvider.CACHE_UPDATED, dataOpsManager.componentManager).onCacheUpdated(query, it)
      }.onError {
        sendTopic(FileFetchProvider.CACHE_UPDATED, dataOpsManager.componentManager).onCacheUpdated(query, listOf())
      } as Promise<Collection<File>>
  }

  override fun cleanCache(query: RemoteQuery<Request, Unit>) {
    cacheState.remove(query)
    sendTopic(FileFetchProvider.CACHE_UPDATED, dataOpsManager.componentManager).onCacheUpdated(query, listOf())
  }

  abstract val responseClass: Class<out Response>

  override val queryClass = RemoteQuery::class.java
}