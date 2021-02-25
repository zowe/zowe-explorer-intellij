package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.application.runReadAction
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.formainframe.vfs.createAttributes
import eu.ibagroup.r2z.Member
import java.io.IOException

class RemoteMemberAttributesServiceFactory : AttributesServiceFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): AttributesService<*, *> {
    return RemoteMemberAttributesService(dataOpsManager)
  }
}

class RemoteMemberAttributesService(
  private val dataOpsManager: DataOpsManager
) : AttributesService<RemoteMemberAttributes, MFVirtualFile> {

  companion object {
    private val fsModel = MFVirtualFileSystem.instance.model
  }

  private val remoteDatasetAttributesService by lazy {
    dataOpsManager.getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
  }

  private var fileToMemberInfoMap = ConcurrentHashMap<MFVirtualFile, Member>()

  private fun getLibrary(libraryFile: MFVirtualFile): MFVirtualFile? {
    val libAttributes = runReadAction { remoteDatasetAttributesService.getAttributes(libraryFile) }
    return if (libAttributes != null) {
      libraryFile
    } else null
  }

  private fun getLibraryAttributes(memberFile: MFVirtualFile): RemoteDatasetAttributes {
    val parent = memberFile.parent
    val attributes = if (parent != null) {
      remoteDatasetAttributesService.getAttributes(parent)
    } else null
    return attributes ?: throw IllegalArgumentException("Cannot find library attributes")
  }

  override fun getOrCreateVirtualFile(attributes: RemoteMemberAttributes): MFVirtualFile {
    val lib = getLibrary(attributes.libraryFile)
    return if (lib != null && lib.isDirectory) {
      fsModel.findOrCreate(this, lib, attributes.name, createAttributes(directory = false)).also {
        fileToMemberInfoMap.putIfAbsent(it, attributes.memberInfo)
      }
    } else throw IOException("Cannot find member")
  }

  override fun getVirtualFile(attributes: RemoteMemberAttributes): MFVirtualFile? {
    val lib = getLibrary(attributes.libraryFile)
    return lib?.findChild(attributes.name)
  }

  override fun getAttributes(file: MFVirtualFile): RemoteMemberAttributes? {
    val lib = file.parent?.let { getLibrary(it) }
    val memberInfo = fileToMemberInfoMap[file]
    return if (lib != null && memberInfo != null) {
      RemoteMemberAttributes(memberInfo, lib)
    } else null
  }

  override fun updateAttributes(file: MFVirtualFile, newAttributes: RemoteMemberAttributes) {
    val oldAttributes = getAttributes(file)
    if (oldAttributes != null) {
      if (oldAttributes.libraryFile != newAttributes.libraryFile) {
        fsModel.moveFileAndReplace(this, oldAttributes.libraryFile, newAttributes.libraryFile)
      }
      if (oldAttributes.memberInfo != newAttributes.memberInfo) {
        fileToMemberInfoMap[file] = newAttributes.memberInfo
      }
    }
  }

  override fun clearAttributes(file: MFVirtualFile) {
    fileToMemberInfoMap.remove(file)
  }

  override val attributesClass = RemoteMemberAttributes::class.java

  override val vFileClass = MFVirtualFile::class.java

}