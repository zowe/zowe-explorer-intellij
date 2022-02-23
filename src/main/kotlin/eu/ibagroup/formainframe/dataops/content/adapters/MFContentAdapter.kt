package eu.ibagroup.formainframe.dataops.content.adapters

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile

interface MFContentAdapter {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<MFContentAdapterFactory>("eu.ibagroup.formainframe.mfContentAdapter")
  }

  fun accepts(file: VirtualFile): Boolean

  fun prepareContentToMainframe (content: ByteArray, file: VirtualFile): ByteArray

  fun adaptContentFromMainframe (content: ByteArray, file: VirtualFile): ByteArray
}
