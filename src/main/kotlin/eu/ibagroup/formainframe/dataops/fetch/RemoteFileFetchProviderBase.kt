package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.AtomicInteger
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.FetchCallback
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.runIfTrue
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import eu.ibagroup.formainframe.utils.sendTopic
import java.util.concurrent.locks.ReentrantLock
import kotlin.streams.toList

abstract class RemoteFileFetchProviderBase<Request : Any, Response : Any, File : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : FileFetchProvider<Request, RemoteQuery<Request>, File> {

  private val lock = ReentrantLock()

  private val cache = mutableMapOf<RemoteQuery<Request>, Collection<File>>()
  private val cacheState = mutableMapOf<RemoteQuery<Request>, Boolean>()

  override fun getCached(query: RemoteQuery<Request>): Collection<File>? {
    return lock(lock) { cacheState[query].runIfTrue { cache[query] } }
  }

  protected abstract fun makeFetchTaskTitle(query: RemoteQuery<Request>): String

  protected abstract fun makeSecondaryTitle(query: RemoteQuery<Request>): String

  protected abstract fun fetchResponse(query: RemoteQuery<Request>): Collection<Response>

  protected abstract fun convertResponseToFile(response: Response): File

  override fun forceReloadSynchronous(query: RemoteQuery<Request>, project: Project?): Collection<File> {
    throw NotImplementedError()
//    var responseFiles: Collection<File>? = null
//    ProgressManager.getInstance().run(object : Task.Modal(explorer.project, makeFetchTaskTitle(query), true) {
//      override fun run(indicator: ProgressIndicator) {
//        indicator.text = makeSecondaryTitle(query)
//        responseFiles = fetchResponse(query).parallelStream().map { convertResponseToFile(it) }.toList()
//      }
//    })
//    return (responseFiles ?: throw CancellationException()).also {
//      cache[query] = it
//      sendCacheUpdatedTopic().onCacheUpdated(query, it)
//    }
  }

  protected abstract fun cleanupUnusedFile(file: File, query: RemoteQuery<Request>)

  open fun compareOldAndNewFile(oldFile: File, newFile: File): Boolean {
    return oldFile.path == newFile.path
  }

  override fun forceReloadAsync(
    query: RemoteQuery<Request>,
    callback: FetchCallback<Collection<File>>,
    project: Project?
  ) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, makeFetchTaskTitle(query), true) {
      override fun run(indicator: ProgressIndicator) {
        callback.onStart()
        try {
          indicator.checkCanceled()
          val fetched = fetchResponse(query)
          indicator.checkCanceled()
          val counter = AtomicInteger(0)
          indicator.fraction = 0.0
          val totalCount = fetched.size
          val files = runWriteActionOnWriteThread {
            fetched.map {
              val file = convertResponseToFile(it)
              indicator.text2 = file.name
              indicator.fraction = counter.incrementAndGet().toDouble() / totalCount
              file
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
          sendTopic(FileFetchProvider.CACHE_UPDATED, dataOpsManager.componentManager).onCacheUpdated(query, files)
          callback.onSuccess(files)
        } catch (t: Throwable) {
          callback.onThrowable(t)
        } finally {
          callback.onFinish()
        }
      }
    })

  }

  override fun cleanCache(query: RemoteQuery<Request>) {
    cacheState.remove(query)
  }

  abstract val responseClass: Class<out Response>

  override val queryClass = RemoteQuery::class.java
}