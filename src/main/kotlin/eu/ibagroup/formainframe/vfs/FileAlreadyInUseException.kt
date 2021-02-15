package eu.ibagroup.formainframe.vfs

class FileAlreadyInUseException(
  private val file: MFVirtualFile
) : ConcurrentModificationException() {
}