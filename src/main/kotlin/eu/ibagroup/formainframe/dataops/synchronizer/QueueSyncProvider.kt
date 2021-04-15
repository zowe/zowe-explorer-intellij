package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.utils.QueueExecutor

class QueueSyncProvider<T : Any>(
  file: VirtualFile,
  progressIndicator: ProgressIndicator,
  saveStrategy: SaveStrategy,
  private val queueExecutor: QueueExecutor<T>,
  synchronizer: ContentSynchronizer,
  removeSyncOnThrowable: (file: VirtualFile, t: Throwable) -> Boolean,
) : DocumentedSyncProviderBase(file, progressIndicator, saveStrategy, synchronizer, removeSyncOnThrowable) {

  override fun beforeSaveDecision() {
    queueExecutor.pause()
  }

  override fun afterSaveDecision() {
    queueExecutor.resume()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is QueueSyncProvider<*>) return false
    if (!super.equals(other)) return false

    if (queueExecutor != other.queueExecutor) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + queueExecutor.hashCode()
    return result
  }


}