package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.Member
import eu.ibagroup.r2z.XIBMDataType

data class RemoteMemberAttributes(
  val memberInfo: Member,
  val libraryFile: MFVirtualFile,
  override var contentMode: XIBMDataType = XIBMDataType.TEXT
) : VFileInfoAttributes {

  override val name
    get() = memberInfo.name

  override val length = 0L


  override fun clone(): VFileInfoAttributes {
    return RemoteMemberAttributes(memberInfo.clone(), libraryFile)
  }

}

fun RemoteMemberAttributes.getLibraryAttributes(dataOpsManager: DataOpsManager): RemoteDatasetAttributes? {
  return dataOpsManager.getAttributesService(RemoteDatasetAttributes::class.java, libraryFile::class.java)
    .getAttributes(libraryFile)
}