/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.services.ErrorSeparatorService
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.runIfTrue
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.utils.sendTopic
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.set
import kotlin.concurrent.withLock

/**
 * Abstract class that represents a base fetch provider for fetching remote files.
 * @param dataOpsManager instance of DataOpsManager service.
 */
abstract class RemoteFileFetchProviderBase<Connection : ConnectionConfigBase, Request : Any, Response : Any, File : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : FileFetchProvider<Request, RemoteQuery<Connection, Request, Unit>, File> {

  private enum class CacheState {
    FETCHED, ERROR
  }

  private val lock = ReentrantLock()

  private val cache = mutableMapOf<RemoteQuery<Connection, Request, Unit>, Collection<File>>()
  private val cacheState = mutableMapOf<RemoteQuery<Connection, Request, Unit>, CacheState>()
  protected var errorMessages = mutableMapOf<RemoteQuery<Connection, Request, Unit>, String>()

  /**
   * Returns successfully cached files.
   * @param query query that identifies the cache.
   * @return collection of cached files.
   */
  override fun getCached(query: RemoteQuery<Connection, Request, Unit>): Collection<File>? {
    return lock.withLock { (cacheState[query] == CacheState.FETCHED).runIfTrue { cache[query] } }
  }

  /**
   * Checks if the cache is valid. Cache must not contain errors.
   * @param query query that identifies the cache.
   * @return true if valid, false if not.
   */
  override fun isCacheValid(query: RemoteQuery<Connection, Request, Unit>): Boolean {
    return lock.withLock { cacheState[query] != CacheState.ERROR }
  }

  /**
   * Returns fetching errors stored in the cache.
   * @param query query that identifies the cache.
   * @return string error message if it exists.
   */
  override fun getFetchedErrorMessage(query: RemoteQuery<Connection, Request, Unit>): String? {
    return lock.withLock { cacheState[query] == CacheState.ERROR }.runIfTrue { errorMessages[query] }
  }

  /**
   * Fetches remote files based on information in query.
   * @param query query with all necessary information to send request.
   * @param progressIndicator progress indicator to display progress of fetching items in UI.
   * @return collection of responses.
   */
  protected abstract fun fetchResponse(
    query: RemoteQuery<Connection, Request, Unit>,
    progressIndicator: ProgressIndicator
  ): Collection<Response>

  protected abstract fun convertResponseToFile(response: Response): File?

  protected abstract fun cleanupUnusedFile(file: File, query: RemoteQuery<Connection, Request, Unit>)

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
   * Make "fetch files" call and convert response to files
   * @param query the query to fetch files
   * @param progressIndicator the progress indicator to cancel the fetch process in case the user wants to
   * @return fetched files
   */
  private fun getFetchedFiles(
    query: RemoteQuery<Connection, Request, Unit>,
    progressIndicator: ProgressIndicator
  ): List<File> {
    val fetched = fetchResponse(query, progressIndicator)
    return runWriteActionInEdtAndWait {
      fetched.mapNotNull {
        convertResponseToFile(it)
      }
    }
  }

  /**
   * Trigger onCacheUpdated event when the operation is completed successfully
   * @param query the query that was used in fetch call
   * @param files the files that were fetched
   */
  private fun publishCacheUpdated(query: RemoteQuery<Connection, Request, Unit>, files: List<File>) {
    sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onCacheUpdated(query, files)
  }

  /**
   * Trigger onFetchCancelled if the fetch operation is cancelled, and onFetchFailure in case the fetch operation is failed.
   * In case of process cancellation, cleans the cache. In case of fetch failure, makes cache in error state, resets the cache
   */
  private fun publishFetchCancelledOrFailed(query: RemoteQuery<Connection, Request, Unit>, throwable: Throwable) {
    if (throwable is ProcessCanceledException) {
      cleanCacheInternal(query, false)
      sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onFetchCancelled(query)
    } else {
      var errorMessage = throwable.message ?: "Error"
      errorMessage = errorMessage.replace("\n", " ")
      if (throwable is CallException) {
        val details = throwable.errorParams?.get("details")
        if (details is List<*>) {
          errorMessage = details[0] as String
        }
        errorMessages[query] =
          service<ErrorSeparatorService>().separateErrorMessage(errorMessage)["error.description"] as String
      } else {
        errorMessages[query] = errorMessage
      }
      cache[query] = listOf()
      cacheState[query] = CacheState.ERROR
      sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onFetchFailure(query, throwable)
    }
  }

  /**
   * Method for reloading remote files. The files are fetched again, the old cache is cleared, the new cache is loaded.
   * All the old files attributes are set to invalid
   * @param query query with all necessary information to send request.
   * @param progressIndicator progress indicator to display progress of fetching items in UI.
   */
  override fun reload(
    query: RemoteQuery<Connection, Request, Unit>,
    progressIndicator: ProgressIndicator
  ) {
    runCatching {
      val files = getFetchedFiles(query, progressIndicator)

      // Cleans up attributes of invalid files
      cache[query]
        ?.parallelStream()
        ?.filter { oldFile ->
          // TODO: does not work correctly on datasets (check VOLSER)
          oldFile.isValid && files.none { compareOldAndNewFile(oldFile, it) }
        }
        ?.toList()
        ?.apply {
          runWriteActionInEdtAndWait {
            forEach { cleanupUnusedFile(it, query) }
          }
        }

      cache[query] = files
      cacheState[query] = CacheState.FETCHED
      files
    }
      .onSuccess { publishCacheUpdated(query, it) }
      .onFailure { publishFetchCancelledOrFailed(query, it) }
  }

  /**
   * Load more children elements. Is triggered when "load more" node is navigated
   * @param query query with all necessary information to send the request
   * @param progressIndicator progress indicator to display progress of fetching items in UI
   */
  override fun loadMode(
    query: RemoteQuery<Connection, Request, Unit>,
    progressIndicator: ProgressIndicator
  ) {
    runCatching {
      val files = getFetchedFiles(query, progressIndicator)
      val newCache = cache[query]?.toMutableList() ?: mutableListOf()
      newCache.addAll(files)
      cache[query] = newCache
      cacheState[query] = CacheState.FETCHED
      files
    }
      .onSuccess { publishCacheUpdated(query, it) }
      .onFailure { publishFetchCancelledOrFailed(query, it) }
  }

  /**
   * Clears the cache with sending the cache change topic.
   * @param query query that identifies the cache.
   * @param sendTopic true if it is necessary to send message in CACHE_CHANGES topic and false otherwise.
   */
  override fun cleanCache(query: RemoteQuery<Connection, Request, Unit>, sendTopic: Boolean) {
    cleanCacheInternal(query, sendTopic)
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
  private fun cleanCacheInternal(query: RemoteQuery<Connection, Request, Unit>, sendTopic: Boolean) {
    cacheState.remove(query)
    if (sendTopic) {
      sendTopic(FileFetchProvider.CACHE_CHANGES, dataOpsManager.componentManager).onCacheCleaned(query)
    }
  }

  abstract val responseClass: Class<out Response>

  override val queryClass = RemoteQuery::class.java
}
