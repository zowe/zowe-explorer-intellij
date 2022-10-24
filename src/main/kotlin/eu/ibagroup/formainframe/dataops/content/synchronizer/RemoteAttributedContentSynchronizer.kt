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
import eu.ibagroup.formainframe.dataops.attributes.ContentEncodingMode
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.utils.*
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.contentEquals
import kotlin.collections.firstOrNull
import kotlin.collections.getOrPut
import kotlin.io.use
import kotlin.collections.set

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
  private val idToPreviousEncoding = ConcurrentHashMap<Int, Charset>()

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
  override fun synchronizeWithRemote(syncProvider: SyncProvider, progressIndicator: ProgressIndicator?) {
    runCatching {
      log.info("Starting synchronization for file ${syncProvider.file.name}.")
      progressIndicator?.text = "Synchronizing file ${syncProvider.file.name} with mainframe"
      val recordId = handlerToStorageIdMap.getOrPut(syncProvider) { successfulStatesStorage.createNewRecord() }
      val attributes = attributesService.getAttributes(syncProvider.file) ?: throw IOException("No Attributes found")

      val ussAttributes = attributes.castOrNull<RemoteUssAttributes>()
      ussAttributes?.let {
        if (!wasFetchedBefore(syncProvider)) {
          checkUssFileTag(it)
        }
      }

      val fetchedRemoteContentBytes = fetchRemoteContentBytes(attributes, progressIndicator)
      val contentAdapter = dataOpsManager.getMFContentAdapter(syncProvider.file)
      val adaptedFetchedBytes = contentAdapter.adaptContentFromMainframe(fetchedRemoteContentBytes, syncProvider.file)

      if (!wasFetchedBefore(syncProvider)) {
        log.info("Setting initial content for file ${syncProvider.file.name}")
        runWriteActionInEdtAndWait { syncProvider.putInitialContent(adaptedFetchedBytes) }
        successfulStatesStorage.writeStream(recordId).use { it.write(adaptedFetchedBytes) }
        fetchedAtLeastOnce.add(syncProvider)
        ussAttributes?.let { idToPreviousEncoding[recordId] = it.ussFileEncoding }
      } else {

        val requiredCharset =
          getRequiredCharset(ussAttributes, syncProvider.file.isBeingEditingNow(), idToPreviousEncoding[recordId])

        val encodingNotChanged = !(ussAttributes != null
            && ussAttributes.ussFileEncoding != idToPreviousEncoding[recordId])

        val fileContent = runReadActionInEdtAndWait { syncProvider.retrieveCurrentContent(requiredCharset) }
        if (fileContent contentEquals adaptedFetchedBytes && encodingNotChanged) {
          return
        }

        val oldStorageBytes = successfulStatesStorage.getBytes(recordId)
        val doUploadContent = if (ussAttributes?.contentEncodingMode == ContentEncodingMode.RELOAD) {
          false
        } else {
            syncProvider.saveStrategy
              .decide(syncProvider.file, oldStorageBytes, adaptedFetchedBytes)
        }

        if (doUploadContent) {
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

        ussAttributes?.let {
          idToPreviousEncoding[recordId] = it.ussFileEncoding
          it.encodingChanged = false
          it.contentEncodingMode = null
        }
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
   * Get the required charset for synchronization.
   * Depending on whether the uss file is fetched from the mainframe or uploaded to the mainframe.
   * @param ussAttributes uss file attributes.
   * @param fileIsEditingNow is the file editing now.
   * @param previousEncoding previous encoding of uss file.
   * @return required charset for uss files or [DEFAULT_TEXT_CHARSET] for other files.
   */
  private fun getRequiredCharset(
    ussAttributes: RemoteUssAttributes?,
    fileIsEditingNow: Boolean,
    previousEncoding: Charset?
  ): Charset {
    ussAttributes?.let {
      return if (fileIsEditingNow) {
        ussAttributes.ussFileEncoding
      } else {
        previousEncoding ?: DEFAULT_BINARY_CHARSET
      }
    }
    return DEFAULT_TEXT_CHARSET
  }
}
