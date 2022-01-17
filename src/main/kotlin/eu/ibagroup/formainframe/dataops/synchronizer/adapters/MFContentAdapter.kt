package eu.ibagroup.formainframe.dataops.synchronizer.adapters

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile

interface MFContentAdapter {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<MFContentAdapterFactory>("eu.ibagroup.formainframe.mfContentAdapter")
  }

  fun accepts(file: VirtualFile): Boolean

  fun performAdaptingToMainframe (content: ByteArray, file: VirtualFile): ByteArray

  fun performAdaptingFromMainframe (content: ByteArray, file: VirtualFile): ByteArray
}
