package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.CancellationException
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.explorer.Explorer
import kotlin.streams.toList

abstract class RemoteFileFetchProviderBase<Request : Any, Response : Any, File : VirtualFile>(protected val explorer: Explorer) :
  FileFetchProvider<Request, RemoteQuery<Request>, File> {

  protected val cache = ConcurrentHashMap<RemoteQuery<Request>, Collection<File>>()

  override fun getCached(query: RemoteQuery<Request>): Collection<File>? {
    return cache[query]
  }

  protected abstract fun makeFetchTaskTitle(query: RemoteQuery<Request>): String

  protected abstract fun makeSecondaryTitle(query: RemoteQuery<Request>): String

  protected abstract fun fetchResponse(query: RemoteQuery<Request>): Collection<Response>

  protected abstract fun convertResponseToFile(response: Response): File

  override fun forceReloadSynchronous(query: RemoteQuery<Request>): Collection<File> {
    var responseFiles: Collection<File>? = null
    ProgressManager.getInstance().run(object : Task.Modal(explorer.project, makeFetchTaskTitle(query), true) {
      override fun run(indicator: ProgressIndicator) {
        indicator.text = makeSecondaryTitle(query)
        responseFiles = fetchResponse(query).parallelStream().map { convertResponseToFile(it) }.toList()
      }
    })
    return (responseFiles ?: throw CancellationException()).also {
      cache[query] = it
      sendCacheUpdatedTopic().onCacheUpdated(query, it)
    }
  }

  override fun forceReloadAsync(query: RemoteQuery<Request>, callback: FetchCallback<File>) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(explorer.project, makeFetchTaskTitle(query), true) {
      override fun run(indicator: ProgressIndicator) {
        callback.onStart()
        try {
          val files = fetchResponse(query).map {
            convertResponseToFile(it)
          }
          cache[query] = files
          sendCacheUpdatedTopic().onCacheUpdated(query, files)
          callback.onSuccess(files)
        } catch (t: Throwable) {
          callback.onThrowable(t)
        } finally {
          callback.onFinish()
        }
      }
    })
  }

  abstract val responseClass: Class<out Response>

  override val queryClass = RemoteQuery::class.java
}