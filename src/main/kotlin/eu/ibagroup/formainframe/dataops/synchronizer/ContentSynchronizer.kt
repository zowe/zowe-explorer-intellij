package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.FetchCallback
import java.io.IOException

enum class AcceptancePolicy {
  IF_EMPTY_ONLY,
  FORCE_REWRITE
}

interface ContentSynchronizer {

  companion object {
    @JvmStatic
    val EP = ExtensionPointName.create<ContentSynchronizerFactory>("eu.ibagroup.formainframe.contentSynchronizer")
  }

  fun accepts(file: VirtualFile): Boolean

  fun isAlreadySynced(file: VirtualFile): Boolean

  @Throws(IOException::class)
  fun enforceSync(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy,
    onSyncEstablished: FetchCallback<Unit>
  )

  @Throws(IOException::class)
  fun enforceSyncIfNeeded(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy,
    onSyncEstablished: FetchCallback<Unit>
  ) {
    if (!isAlreadySynced(file)) {
      enforceSync(file, acceptancePolicy, saveStrategy, onSyncEstablished)
    }
  }

  @Throws(IOException::class)
  fun removeSync(file: VirtualFile)

}