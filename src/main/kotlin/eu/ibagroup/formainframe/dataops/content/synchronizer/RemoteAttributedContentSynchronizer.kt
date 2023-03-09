/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.utils.*
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

private const val SUCCESSFUL_CONTENT_STORAGE_NAME_PREFIX = "sync_storage_"
private val log = logger<RemoteAttributedContentSynchronizer<*>>()

/**
 * Base implementation of content synchronizer.
 * Works with filesystem and file document but not interact with mainframe.
 * @author Valentine Krus
 */
abstract class RemoteAttributedContentSynchronizer<FAttributes : FileAttributes>(
  val dataOpsManager: DataOpsManager
) : ContentSynchronizer {

  /**
   * [FileAttributes] implementation class
   */
  abstract val attributesClass: Class<out FAttributes>

  /**
   * File entity (or type) name such as "members" or "jobs".
   * Used as [ContentStorage] postfix in most cases.
   */
  abstract val entityName: String

  // TODO: doc if needed.
  val attributesService by lazy { dataOpsManager.getAttributesService(attributesClass, vFileClass) }
  private val successfulStatesStorage by lazy { ContentStorage(SUCCESSFUL_CONTENT_STORAGE_NAME_PREFIX + entityName) }
  private val handlerToStorageIdMap = ConcurrentHashMap<SyncProvider, Int>()
  private val idToBinaryFileMap = ConcurrentHashMap<Int, VirtualFile>()
  private val fetchedAtLeastOnce = ConcurrentHashMap.newKeySet<SyncProvider>()

  /**
   * Abstract method for fetching content from mainframe.
   * @param attributes [FileAttributes] instance that contains all information that identifies a file to get content from.
   * @param progressIndicator indicator to reflect uploading process status.
   */
  @Throws(Throwable::class)
  protected abstract fun fetchRemoteContentBytes(
    attributes: FAttributes,
    progressIndicator: ProgressIndicator?
  ): ByteArray

  /**
   * Abstract method for uploading content to mainframe.
   * @param attributes [FileAttributes] instance that contains all information that identifies a file to update.
   * @param newContentBytes content to upload to mainframe.
   * @param progressIndicator indicator to reflect uploading process status.
   */
  @Throws(Throwable::class)
  protected abstract fun uploadNewContent(
    attributes: FAttributes,
    newContentBytes: ByteArray,
    progressIndicator: ProgressIndicator?
  )

  /**
   * Base implementation of [ContentSynchronizer.accepts] method for each content synchronizer.
   */
  override fun accepts(file: VirtualFile): Boolean {
    return vFileClass.isAssignableFrom(file::class.java) && attributesService.getAttributes(file) != null
  }

  /**
   * Checks if the file was fetched at least one at the current moment
   * @param syncProvider instance of [SyncProvider] class that contains necessary data and handler.
   * @return true if it was fetched before or false otherwise
   */
  private fun wasFetchedBefore(syncProvider: SyncProvider): Boolean {
    return fetchedAtLeastOnce.firstOrNull { syncProvider == it } != null
  }

  /**
   * Base implementation of [ContentSynchronizer.synchronizeWithRemote] method for each synchronizer.
   * Doesn't need to be overridden in most cases
   * @see ContentSynchronizer.synchronizeWithRemote
   */
  override fun synchronizeWithRemote(
    syncProvider: SyncProvider,
    progressIndicator: ProgressIndicator?,
    forceReload: Boolean
  ) {
    runCatching {
      log.info("Starting synchronization for file ${syncProvider.file.name}.")
      progressIndicator?.text = "Synchronizing file ${syncProvider.file.name} with mainframe"
      val recordId = handlerToStorageIdMap.getOrPut(syncProvider) { successfulStatesStorage.createNewRecord() }
      val attributes = attributesService.getAttributes(syncProvider.file) ?: throw IOException("No Attributes found")

      val ussAttributes = attributes.castOrNull<RemoteUssAttributes>()
      if (!wasFetchedBefore(syncProvider)) {
        ussAttributes?.let {
          checkUssFileTag(it)
        }
      }
      val currentCharset = ussAttributes?.charset ?: DEFAULT_TEXT_CHARSET

      val fetchedRemoteContentBytes = fetchRemoteContentBytes(attributes, progressIndicator)
      val contentAdapter = dataOpsManager.getMFContentAdapter(syncProvider.file)
      val adaptedFetchedBytes = contentAdapter.adaptContentFromMainframe(fetchedRemoteContentBytes, syncProvider.file)

      if (!wasFetchedBefore(syncProvider)) {
        log.info("Setting initial content for file ${syncProvider.file.name}")
        runWriteActionInEdtAndWait { syncProvider.putInitialContent(adaptedFetchedBytes) }
        changeFileEncodingTo(syncProvider.file, currentCharset)
        initLineSeparator(syncProvider.file)
        successfulStatesStorage.writeStream(recordId).use { it.write(adaptedFetchedBytes) }
        fetchedAtLeastOnce.add(syncProvider)
      } else {

        val fileContent = runReadActionInEdtAndWait { syncProvider.retrieveCurrentContent() }

        if (!(fileContent contentEquals adaptedFetchedBytes)) {
          val oldStorageBytes = successfulStatesStorage.getBytes(recordId)
          val doUploadContent =
            syncProvider.saveStrategy.decide(syncProvider.file, oldStorageBytes, adaptedFetchedBytes)

          if (doUploadContent && !forceReload) {
            log.info("Save strategy decided to forcefully update file content on mainframe.")
            val newContentPrepared = contentAdapter.prepareContentToMainframe(fileContent, syncProvider.file)
            runWriteActionInEdtAndWait { syncProvider.loadNewContent(newContentPrepared) }
            uploadNewContent(attributes, newContentPrepared, progressIndicator)
            successfulStatesStorage.writeStream(recordId).use { it.write(newContentPrepared) }
          } else {
            log.info("Save strategy decided to accept remote file content.")
            successfulStatesStorage.writeStream(recordId).use { it.write(adaptedFetchedBytes) }
            runWriteActionInEdt { syncProvider.loadNewContent(adaptedFetchedBytes) }
          }
        } else { /*do nothing*/
        }
        syncCharsetsIfNeeded(syncProvider.file, currentCharset)
      }
    }
      .onSuccess {
        syncProvider.onSyncSuccess()
      }
      .onFailure {
        syncProvider.onThrowable(it)
      }
  }

  /**
   * Base implementation of [ContentSynchronizer.successfulContentStorage] method for each content synchronizer.
   */
  override fun successfulContentStorage(syncProvider: SyncProvider): ByteArray {
    val recordId = handlerToStorageIdMap[syncProvider]
    return recordId?.let { successfulStatesStorage.getBytes(it) } ?: ByteArray(0)
  }

  /**
   * Base implementation of [ContentSynchronizer.isFileSyncPossible] method for each content synchronizer.
   */
  override fun isFileSyncPossible(syncProvider: SyncProvider): Boolean {
    val attributes = attributesService.getAttributes(syncProvider.file) ?: return false
    runCatching {
      uploadNewContent(attributes, successfulContentStorage(syncProvider), null)
    }.onFailure {
      return false
    }
    return true
  }

  /**
   * Synchronizes the current charset with the file charset if needed.
   * @param file virtual file to sync.
   * @param currentCharset current content charset.
   */
  private fun syncCharsetsIfNeeded(file: VirtualFile, currentCharset: Charset) {
    if (currentCharset != file.charset) {
      changeFileEncodingTo(file, currentCharset)
    }
  }
}
