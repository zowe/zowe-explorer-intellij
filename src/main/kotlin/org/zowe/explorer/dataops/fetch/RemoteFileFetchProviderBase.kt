/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Query
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.services.ErrorSeparatorService
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.runIfTrue
import org.zowe.explorer.utils.runWriteActionOnWriteThread
import org.zowe.explorer.utils.sendTopic
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.set
import kotlin.concurrent.withLock
import kotlin.streams.toList

/**
 * Abstract class that represents a base fetch provider for fetching remote files.
 * @param dataOpsManager instance of DataOpsManager service.
 */
abstract class RemoteFileFetchProviderBase<Request : Any, Response : Any, File : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : FileFetchProvider<Request, RemoteQuery<Request, Unit>, File> {

  private enum class CacheState {
    FETCHED, ERROR
  }

  private val lock = ReentrantLock()

  private val cache = mutableMapOf<RemoteQuery<Request, Unit>, Collection<File>>()
  private val cacheState = mutableMapOf<RemoteQuery<Request, Unit>, CacheState>()
  protected var errorMessages = mutableMapOf<RemoteQuery<Request, Unit>, String>()

  /**
   * Returns successfully cached files.
   * @param query query that identifies the cache.
   * @return collection of cached files.
   */
  override fun getCached(query: RemoteQuery<Request, Unit>): Collection<File>? {
    return lock.withLock { (cacheState[query] == CacheState.FETCHED).runIfTrue { cache[query] } }
  }

  /**
   * Checks if the cache is valid. Cache must not contain errors.
   * @param query query that identifies the cache.
   * @return true if valid, false if not.
   */
  override fun isCacheValid(query: RemoteQuery<Request, Unit>): Boolean {
    return lock.withLock { cacheState[query] != CacheState.ERROR }
  }

  /**
   * Returns fetching errors stored in the cache.
   * @param query query that identifies the cache.
   * @return string error message if it exists.
   */
  override fun getFetchedErrorMessage(query: RemoteQuery<Request, Unit>): String? {
    return lock.withLock { cacheState[query] == CacheState.ERROR }.runIfTrue { errorMessages[query] }
  }

  /**
   * Fetches remote files based on information in query.
   * @param query query with all necessary information to send request.
   * @param progressIndicator progress indicator to display progress of fetching items in UI.
   * @return collection of responses.
   */
  protected abstract fun fetchResponse(
    query: RemoteQuery<Request, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<Response>

  protected abstract fun convertResponseToFile(response: Response): File?

  protected abstract fun cleanupUnusedFile(file: File, query: RemoteQuery<Request, Unit>)

  /**
   * Compares the old and new file by their paths.
   * @param oldFile old file.
   * @param newFile new file.
   * @return true if it matches, false if it doesn't match.
   */
  open fun compareOldAndNewFile(oldFile: File, newFile: File): Boolean {
    return oldFile.path == newFile.path
  }

  /**
   * Method for reloading remote files. The files are fetched again, the old cache is cleared, the new cache is loaded.
   * @param query query with all necessary information to send request.
   * @param progressIndicator progress indicator to display progress of fetching items in UI.
   */
  override fun reload(
    query: RemoteQuery<Request, Unit>,
    progressIndicator: ProgressIndicator
  ) {
    runCatching {
      val needToUpdateFiles = query.castOrNull<BatchedRemoteQuery<*>>()?.let { it.fetchNeeded && it.alreadyFetched > 0 } == true
      val fetched = fetchResponse(query, progressIndicator)
      val files = runWriteActionOnWriteThread {
        fetched.mapNotNull {
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
      if (needToUpdateFiles) {
        val newCache = cache[query]?.toMutableList() ?: mutableListOf()
        newCache.addAll(files)
        cache[query] = newCache
      } else {
        cache[query] = files
      }
      cacheState[query] = CacheState.FETCHED
      files
    }.onSuccess {
      sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onCacheUpdated(query, it)
    }.onFailure {
      if (it is ProcessCanceledException) {
        cleanCacheInternal(query, false)
        sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onFetchCancelled(query)
      } else {
        if (it is CallException) {
          val details = it.errorParams?.get("details")
          var errorMessage = it.message ?: "Error"
          if (details is List<*>) {
            errorMessage = details[0] as String
          }
          errorMessages[query] =
            service<ErrorSeparatorService>().separateErrorMessage(errorMessage)["error.description"] as String
        } else {
          val errorMessage = it.message ?: "Error"
          errorMessages[query] = errorMessage
        }
        cache[query] = listOf()
        cacheState[query] = CacheState.ERROR
        sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onFetchFailure(query, it)
      }
    }
  }

  /**
   * Clears the cache with sending the cache change topic.
   * @param query query that identifies the cache.
   */
  override fun cleanCache(query: RemoteQuery<Request, Unit>) {
    cleanCacheInternal(query, true)
  }

  /** @see FileFetchProvider.getRealQueryInstance */
  override fun <Q : Query<Request, Unit>> getRealQueryInstance(query: Q?): Q? {
    val queryClass = query?.javaClass ?: return null
    return cache.keys.find { it == query }.castOrNull(queryClass)
  }

  /**
   * Clears the cache and sends a cache change topic if the flag is set.
   * @param query query that identifies the cache.
   * @param sendTopic topic send flag.
   */
  private fun cleanCacheInternal(query: RemoteQuery<Request, Unit>, sendTopic: Boolean) {
    cacheState.remove(query)
    if (sendTopic) {
      sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onCacheCleaned(query)
    }
  }

  abstract val responseClass: Class<out Response>

  override val queryClass = RemoteQuery::class.java
}
