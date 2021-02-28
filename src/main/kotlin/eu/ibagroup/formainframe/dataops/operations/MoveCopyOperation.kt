package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile

class MoveCopyOperation(
  sources: List<VirtualFile>,
  destination: VirtualFile,
  val newName: String,
  val deleteSource: Boolean,
  val forceOverwriting: Boolean
) : Operation {

  val sources: List<VirtualFile>
    get() = files.dropLast(1)

  val destination: VirtualFile
    get() = files.last()

  override val files = let {
    if (!destination.isDirectory) {
      throw IllegalArgumentException("Destination file ${destination.path} is not a directory")
    }
    sources + destination
  }

}