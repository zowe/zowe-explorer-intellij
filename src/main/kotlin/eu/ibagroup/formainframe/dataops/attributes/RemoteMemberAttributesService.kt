package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.r2z.Member
import eu.ibagroup.r2z.XIBMDataType

class RemoteMemberAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteMemberAttributesService(dataOpsManager)
  }
}

class RemoteMemberAttributesService(
  val dataOpsManager: DataOpsManager
) :
  DependentFileAttributesService<RemoteMemberAttributes, Member, RemoteDatasetAttributes, MFVirtualFile>(dataOpsManager) {

  companion object {
    private val fsModel = MFVirtualFileSystem.instance.model
  }

  override val findOrCreateFileInVFSModel = fsModel::findOrCreate
  override val moveFileAndReplaceInVFSModel = fsModel::moveFileAndReplace


  override fun buildAttributes(
    info: Member, file: MFVirtualFile, contentMode: XIBMDataType?
  ): RemoteMemberAttributes {
    return RemoteMemberAttributes(info, file, contentMode ?: XIBMDataType(XIBMDataType.Type.TEXT))
  }


  override val attributesClass = RemoteMemberAttributes::class.java
  override val vFileClass = MFVirtualFile::class.java
  override val parentAttributesClass = RemoteDatasetAttributes::class.java
}