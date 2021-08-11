package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.SpoolFile
import eu.ibagroup.r2z.XIBMDataType

class RemoteSpoolFileAttributes(
  override val info: SpoolFile,
  override val parentFile: MFVirtualFile,
  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT)
) : DependentFileAttributes<SpoolFile, MFVirtualFile> {
  override val name: String
    get() = info.ddName
  override val length: Long
    get() = 0L

  override fun clone(): FileAttributes {
    return RemoteSpoolFileAttributes(info.clone(), parentFile)
  }

}