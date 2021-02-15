package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

abstract class MFVirtualFileEventBase(
  protected val vFile: MFVirtualFile,
  val parent: MFVirtualFile,
  val attributes: FileAttributes,
  isFromRefresh: Boolean,
  requestor: Any? = null
) : VFileEvent(requestor, isFromRefresh) {

  override fun computePath() = file.path

  override fun getFile() = vFile

  override fun getFileSystem() = file.fileSystem

}