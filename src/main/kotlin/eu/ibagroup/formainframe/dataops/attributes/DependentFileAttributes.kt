package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFile

interface DependentFileAttributes<InfoType, VFile: VirtualFile>: FileAttributes{
  val parentFile: VFile
  val info: InfoType
}