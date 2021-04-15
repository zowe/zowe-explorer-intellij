package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.util.io.FileAttributes
import java.util.*

class MFVirtualFileCreated(
  vFile: MFVirtualFile,
  parent: MFVirtualFile,
  attributes: FileAttributes,
  isFromRefresh: Boolean,
  requestor: Any? = null
) : MFVirtualFileEventBase(vFile, parent, attributes, isFromRefresh, requestor) {

  override fun isValid() = parent.children?.contains(vFile) ?: false

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return true
  }

  override fun hashCode(): Int {
    return Objects.hash(vFile, parent, attributes, isFromRefresh, requestor)
  }


}