package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsComponentFactory

/**
 * Factory interface for creating [ContentSynchronizer] instances.
 * @author Valentine Krus
 */
interface ContentSynchronizerFactory: DataOpsComponentFactory<ContentSynchronizer>

/**
 * Base interface for synchronization specific file with mainframe.
 * @author Valentine Krus
 */
interface ContentSynchronizer {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ContentSynchronizerFactory>("eu.ibagroup.formainframe.contentSynchronizer")
  }

  /**
   * Virtual file class (MFVirtualFile in most cases).
   */
  val vFileClass: Class<out VirtualFile>

  /**
   * Checks if current implementation of content synchronizer can synchronize passed file with mainframe.
   * @param file virtual file to check compatibility with synchronizer.
   * @return true if file can be synchronized by current instance or false otherwise.
   */
  fun accepts(file: VirtualFile): Boolean

  /**
   * Performs synchronization of file with mainframe.
   * @param syncProvider instance of [SyncProvider] class that contains necessary data and handler to start synchronize.
   * @param progressIndicator indicator to reflect synchronization process (not required).
   */
  fun synchronizeWithRemote(syncProvider: SyncProvider, progressIndicator: ProgressIndicator? = null)
}
