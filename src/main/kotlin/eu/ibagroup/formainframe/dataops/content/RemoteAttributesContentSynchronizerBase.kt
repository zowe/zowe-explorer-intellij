package eu.ibagroup.formainframe.dataops.content

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBus
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.dataops.attributes.MFRemoteFileAttributes
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.utils.ContentStorage
import eu.ibagroup.formainframe.utils.Execution
import eu.ibagroup.formainframe.utils.runWriteActionOnWriteThread
import java.io.IOException

private const val SUCCESSFUL_CONTENT_STORAGE_NAME_PREFIX = "sync_storage_"

abstract class RemoteAttributesContentSynchronizerBase<Attributes : VFileInfoAttributes>(
  messageBus: MessageBus,
  parentDisposable: Disposable
) : AbstractAttributedContentSynchronizer<Attributes>(messageBus, parentDisposable) {

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
  ): Execution<Unit, Unit> {
    return object : Execution<Unit, Unit> {

      private val recordId = fileToStorageIdMap.getOrPut(file) {
        successfulStatesStorage.createNewRecord()
      }

      @Volatile
      private var neverFetchedBefore = true

      private val attributes
        get() = attributesService.getAttributes(file)
          ?: throw IOException("No Attributes found")

      override fun execute(input: Unit) {
        val remoteContentBytes = fetchRemoteContentBytes(attributes)
        if (neverFetchedBefore) {
          runWriteActionOnWriteThread {
            file.getOutputStream(this@RemoteAttributesContentSynchronizerBase).use {
              it.write(remoteContentBytes)
            }
            successfulStatesStorage.writeStream(recordId).use {
              it.write(remoteContentBytes)
            }
            neverFetchedBefore = false
          }
        }
        val fileBytes = file.contentsToByteArray()
        if (saveStrategy(successfulStatesStorage.getBytes(recordId), fileBytes, file)) {
          uploadNewContent(attributes, fileBytes)
        }
      }

      override fun receive(result: Unit) {
      }

      override fun onThrowable(input: Unit, throwable: Throwable) {
        //TODO
        throwable.printStackTrace()
      }

    }
  }
}