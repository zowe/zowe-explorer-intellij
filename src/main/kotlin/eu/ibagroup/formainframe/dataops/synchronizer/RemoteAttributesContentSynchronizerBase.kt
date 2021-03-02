package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.FetchCallback
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.utils.ContentStorage
import eu.ibagroup.formainframe.utils.Execution
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
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
  protected abstract fun fetchRemoteContentBytes(attributes: Attributes): ByteArray

  @Throws(Throwable::class)
  protected abstract fun uploadNewContent(attributes: Attributes, newContentBytes: ByteArray)

  private val fileToStorageIdMap = ConcurrentHashMap<VirtualFile, Int>()

  override fun buildExecution(
    file: VirtualFile,
    saveStrategy: SaveStrategy
  ): Execution<FetchCallback<Unit>, Unit> {
    return object : Execution<FetchCallback<Unit>, Unit> {

      private val recordId = fileToStorageIdMap.getOrPut(file) {
        successfulStatesStorage.createNewRecord()
      }

      @Volatile
      private var neverFetchedBefore = true

      private val attributes
        get() = attributesService.getAttributes(file)
          ?: throw IOException("No Attributes found")

      override fun execute(input: FetchCallback<Unit>) {
        if (neverFetchedBefore) {
          input.onStart()
        }
        val remoteContentBytes = fetchRemoteContentBytes(attributes)
        if (neverFetchedBefore) {
          runWriteActionOnWriteThread {
            file.getOutputStream(this@RemoteAttributesContentSynchronizerBase).use {
              it.write(remoteContentBytes)
            }
            successfulStatesStorage.writeStream(recordId).use {
              it.write(remoteContentBytes)
            }
          }
          input.onSuccess(Unit)
          neverFetchedBefore = false
        } else {
          val fileBytes = file.contentsToByteArray()
          if (saveStrategy.decide(file, successfulStatesStorage.getBytes(recordId), fileBytes)) {
            uploadNewContent(attributes, fileBytes)
          }
        }
      }

      override fun receive(result: Unit) {
      }

      override fun onThrowable(input: FetchCallback<Unit>, throwable: Throwable) {
        input.onThrowable(throwable)
      }

    }
  }
}