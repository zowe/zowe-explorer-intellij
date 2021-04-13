package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.utils.castOrNull
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class DocumentedSyncProviderBase(
  override val file: VirtualFile,
  override val progressIndicator: ProgressIndicator,
  override val saveStrategy: SaveStrategy,
  protected val synchronizer: ContentSynchronizer,
  protected val removeSyncOnThrowable: (file: VirtualFile, t: Throwable) -> Boolean
) : SyncProvider {

  private val fileDocumentManager = FileDocumentManager.getInstance()
  private val document
    get() = fileDocumentManager.getDocument(file) ?: throw IOException("Unsupported file ${file.path}")

  private val isInitialContentSet = AtomicBoolean(false)

  override val isReadOnly: Boolean
    get() = !document.isWritable

  override fun putInitialContent(content: ByteArray) {
    if (isInitialContentSet.compareAndSet(false, true)) {
      runCatching {
        file.getOutputStream(null).use {
          it.write(content)
        }
        document.castOrNull<DocumentImpl>()?.setAcceptSlashR(true)
        val wasReadOnly = isReadOnly
        if (wasReadOnly) {
          document.setReadOnly(false)
        }
        document.setText(String(content))
        if (wasReadOnly) {
          document.setReadOnly(true)
        }
      }.onFailure {
        isInitialContentSet.set(false)
      }
    }
  }

  override fun loadNewContent(content: ByteArray) {
    document.setText(String(content))
  }

  override fun retrieveCurrentContent(): ByteArray {
    return document.text.toByteArray()
  }

  private val startLock = ReentrantLock()
  private val startCondition = startLock.newCondition()

  override fun notifySyncStarted() {
    startLock.withLock { startCondition.signalAll() }
  }

  override fun waitForSyncStarted() {
    startLock.withLock { startCondition.await() }
  }

  override fun onThrowable(t: Throwable) {
    if (removeSyncOnThrowable(file, t)) {
      synchronizer.removeSync(file)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DocumentedSyncProviderBase) return false

    if (file != other.file) return false
    if (progressIndicator != other.progressIndicator) return false
    if (saveStrategy != other.saveStrategy) return false
    if (synchronizer != other.synchronizer) return false
    if (removeSyncOnThrowable != other.removeSyncOnThrowable) return false
    if (fileDocumentManager != other.fileDocumentManager) return false
    if (startLock != other.startLock) return false
    if (startCondition != other.startCondition) return false

    return true
  }

  override fun hashCode(): Int {
    var result = file.hashCode()
    result = 31 * result + progressIndicator.hashCode()
    result = 31 * result + saveStrategy.hashCode()
    result = 31 * result + synchronizer.hashCode()
    result = 31 * result + removeSyncOnThrowable.hashCode()
    result = 31 * result + fileDocumentManager.hashCode()
    result = 31 * result + startLock.hashCode()
    result = 31 * result + startCondition.hashCode()
    return result
  }


}