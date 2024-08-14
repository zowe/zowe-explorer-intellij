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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.service.SyncProcessService
import eu.ibagroup.formainframe.dataops.content.service.isFileSyncingNow
import eu.ibagroup.formainframe.editor.FileContentChangeListener
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFBulkFileListener
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import java.io.IOException
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

  init {
    subscribe(
      componentManager = dataOpsManager.componentManager,
      topic = FileContentChangeListener.FILE_CONTENT_CHANGED,
      handler = object : FileContentChangeListener {
        override fun onUpdate(file: VirtualFile) {
          needToUpload.add(DocumentedSyncProvider(file))
        }
      }
    )
    subscribe(
      componentManager = ApplicationManager.getApplication(),
      topic = MFVirtualFileSystem.MF_VFS_CHANGES_TOPIC,
      handler = object : MFBulkFileListener {
        override fun after(events: List<VFileEvent>) {
          events.filterIsInstance<VFileDeleteEvent>().forEach { event ->
            fetchedAtLeastOnce.removeIf { it.file == event.file }
          }
        }
      }
    )
  }

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
  private val needToUpload = ConcurrentHashMap.newKeySet<SyncProvider>()

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
    progressIndicator: ProgressIndicator?
  ) {
    runCatching {
      log.info("Starting synchronization for file ${syncProvider.file.name}.")
      if (isFileSyncingNow(syncProvider.file)) {
        log.info("Synchronization is interrupted because it is already running for file ${syncProvider.file.name}.")
        return
      }
      progressIndicator?.text = "Synchronizing file ${syncProvider.file.name} with mainframe"
      progressIndicator?.let {
        SyncProcessService.instance.startFileSync(syncProvider.file, it)
      }

      val recordId = handlerToStorageIdMap.getOrPut(syncProvider) { successfulStatesStorage.createNewRecord() }
      val attributes = attributesService.getAttributes(syncProvider.file) ?: throw IOException("No Attributes found")
      val ussAttributes = attributes.castOrNull<RemoteUssAttributes>()

      val fetchedRemoteContentBytes = fetchRemoteContentBytes(attributes, progressIndicator)
      val contentAdapter = dataOpsManager.getMFContentAdapter(syncProvider.file)
      val adaptedFetchedBytes = contentAdapter.adaptContentFromMainframe(fetchedRemoteContentBytes, syncProvider.file)

      if (!wasFetchedBefore(syncProvider)) {
        log.info("Setting initial content for file ${syncProvider.file.name}")
        ussAttributes?.let { checkUssFileTag(it) }
        runWriteActionInEdtAndWait { syncProvider.putInitialContent(adaptedFetchedBytes) }
        successfulStatesStorage.writeStream(recordId).use { it.write(adaptedFetchedBytes) }
        fetchedAtLeastOnce.add(syncProvider)
      } else {

        val fileContent = runReadAction { syncProvider.retrieveCurrentContent() }

        if (!(fileContent contentEquals adaptedFetchedBytes)) {
          val oldStorageBytes = successfulStatesStorage.getBytes(recordId)
          val doUploadContent =
            syncProvider.saveStrategy.decide(syncProvider.file, oldStorageBytes, adaptedFetchedBytes)

          if (doUploadContent && isFileUploadNeeded(syncProvider)) {
            log.info("Save strategy decided to forcefully update file content on mainframe.")
            val newContentPrepared = contentAdapter.prepareContentToMainframe(fileContent, syncProvider.file)
            runWriteActionInEdtAndWait { syncProvider.loadNewContent(newContentPrepared) }
            uploadNewContent(attributes, newContentPrepared, progressIndicator)
            successfulStatesStorage.writeStream(recordId).use { it.write(newContentPrepared) }
          } else {
            log.info("Save strategy decided to accept remote file content.")
            successfulStatesStorage.writeStream(recordId).use { it.write(adaptedFetchedBytes) }
            runWriteActionInEdtAndWait { syncProvider.loadNewContent(adaptedFetchedBytes) }
          }
        } else { /*do nothing*/
        }
      }
      needToUpload.remove(syncProvider)
    }
      .onSuccess {
        syncProvider.onSyncSuccess()
      }
      .onFailure {
        syncProvider.onThrowable(it)
      }
      .also {
        SyncProcessService.instance.stopFileSync(syncProvider.file)
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
   * Base implementation of [ContentSynchronizer.isFileUploadNeeded] method for each content synchronizer.
   */
  override fun isFileUploadNeeded(syncProvider: SyncProvider): Boolean {
    return needToUpload.firstOrNull { syncProvider == it } != null
  }

  /**
   * Base implementation of [ContentSynchronizer.markAsNotNeededForSync] method for each content synchronizer.
   */
  override fun markAsNotNeededForSync(syncProvider: SyncProvider) {
    needToUpload.remove(syncProvider)
  }
}
