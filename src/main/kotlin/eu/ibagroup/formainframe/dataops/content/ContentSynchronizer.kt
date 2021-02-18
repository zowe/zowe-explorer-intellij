package eu.ibagroup.formainframe.dataops.content

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException

enum class AcceptancePolicy {
  IF_EMPTY_ONLY,
  FORCE_REWRITE
}

interface ContentSynchronizer {

  companion object {
    @JvmStatic
    val EP = ExtensionPointName.create<ContentSynchronizer>("eu.ibagroup.formainframe.contentSynchronizer")
  }

  fun accepts(file: VirtualFile): Boolean

  fun isAlreadySynced(file: VirtualFile): Boolean

  @Throws(IOException::class)
  fun enforceSync(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy
  )

  @Throws(IOException::class)
  fun removeSync(file: VirtualFile)

}