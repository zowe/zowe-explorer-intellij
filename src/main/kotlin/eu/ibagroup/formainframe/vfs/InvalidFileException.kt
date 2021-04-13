package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.vfs.VirtualFile
import java.io.FileNotFoundException

class InvalidFileException(file: VirtualFile) : FileNotFoundException(
  "${file.name} not found on fs ${file.fileSystem}"
)