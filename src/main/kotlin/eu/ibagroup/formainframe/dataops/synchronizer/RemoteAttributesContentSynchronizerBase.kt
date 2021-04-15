package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.utils.ContentStorage
import eu.ibagroup.formainframe.utils.runReadActionInEdtAndWait
import eu.ibagroup.formainframe.utils.runWriteActionInEdt
import java.io.IOException

private const val SUCCESSFUL_CONTENT_STORAGE_NAME_PREFIX = "sync_storage_"

abstract class RemoteAttributesContentSynchronizerBase<Attributes : VFileInfoAttributes>(
  dataOpsManager: DataOpsManager
) : AbstractAttributedContentSynchronizer<Attributes>(dataOpsManager) {

  protected abstract val storageNamePostfix: String

  private val successfulStatesStorage by lazy {
    ContentStorage(SUCCESSFUL_CONTENT_STORAGE_NAME_PREFIX + storageNamePostfix)
  }

  init {
    Disposer.register(disposableQueues, successfulStatesStorage)
  }

  @Throws(Throwable::class)
  protected abstract fun fetchRemoteContentBytes(
    attributes: Attributes,
    progressIndicator: ProgressIndicator?
  ): ByteArray

  @Throws(Throwable::class)
  protected abstract fun uploadNewContent(attributes: Attributes, newContentBytes: ByteArray)

  private val handlerToStorageIdMap = ConcurrentHashMap<SyncProvider, Int>()
  private val fetchedAtLeastOnce = ConcurrentHashMap.newKeySet<SyncProvider>()

  override fun removeSync(file: VirtualFile) {
    fileToConfigMap[file]?.let { fetchedAtLeastOnce.remove(it) }
    super.removeSync(file)
  }

  override fun execute(syncProvider: SyncProvider) {
    val neverFetchedBefore = !fetchedAtLeastOnce.contains(syncProvider)
    runCatching {
      val recordId = handlerToStorageIdMap.getOrPut(syncProvider) {
        successfulStatesStorage.createNewRecord()
      }
      val attributes = attributesService.getAttributes(syncProvider.file)
        ?: throw IOException("No Attributes found")
      val indicator = if (neverFetchedBefore) {
        syncProvider.progressIndicator
      } else {
        null
      }
      val fetchedRemoteContentBytes = fetchRemoteContentBytes(attributes, indicator)
      if (neverFetchedBefore) {
        runWriteActionInEdt {
          syncProvider.putInitialContent(fetchedRemoteContentBytes)
        }
        successfulStatesStorage.writeStream(recordId).use {
          it.write(fetchedRemoteContentBytes)
        }
        fetchedAtLeastOnce.add(syncProvider)
        syncProvider.notifySyncStarted()
      } else {
        val fileContent = runReadActionInEdtAndWait { syncProvider.retrieveCurrentContent() }
        if (fileContent contentEquals fetchedRemoteContentBytes) {
          successfulStatesStorage.writeStream(recordId).use {
            it.write(fileContent)
          }
          return
        }
        val oldStorageBytes = successfulStatesStorage.getBytes(recordId)
        syncProvider.beforeSaveDecision()
        val doUploadContent = syncProvider.saveStrategy
          .decide(syncProvider.file, oldStorageBytes, fetchedRemoteContentBytes)
        syncProvider.afterSaveDecision()
        if (doUploadContent) {
          uploadNewContent(attributes, fileContent)
          successfulStatesStorage.writeStream(recordId).use {
            it.write(fileContent)
          }
        } else {
          successfulStatesStorage.writeStream(recordId).use {
            it.write(fetchedRemoteContentBytes)
          }
          runWriteActionInEdt {
            syncProvider.loadNewContent(fetchedRemoteContentBytes)
          }
        }
      }
    }.onFailure {
      onThrowable(it, syncProvider, neverFetchedBefore)
    }
  }

  private fun onThrowable(t: Throwable, syncProvider: SyncProvider, neverFetchedBefore: Boolean) {
    if (neverFetchedBefore) {
      syncProvider.notifySyncStarted()
    }
    syncProvider.onThrowable(t)
  }

}