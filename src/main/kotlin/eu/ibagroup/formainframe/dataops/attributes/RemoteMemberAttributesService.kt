package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.application.runReadAction
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.formainframe.vfs.createAttributes
import eu.ibagroup.r2z.Member
import eu.ibagroup.r2z.XIBMDataType
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
  private var fileToContentTypeMap = ConcurrentHashMap<MFVirtualFile, XIBMDataType>()

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
        //fileToMemberInfoMap.putIfAbsent(it, attributes.memberInfo)
        fileToMemberInfoMap[it] = attributes.memberInfo
        fileToContentTypeMap[it] = attributes.contentMode
        sendTopic(
          topic = AttributesService.ATTRIBUTES_CHANGED,
          componentManager = dataOpsManager.componentManager
        ).onCreate(attributes, it)
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
      RemoteMemberAttributes(memberInfo, lib, fileToContentTypeMap[file] ?: XIBMDataType.TEXT)
    } else null
  }

  override fun updateAttributes(file: MFVirtualFile, newAttributes: RemoteMemberAttributes) {
    val oldAttributes = getAttributes(file)
    if (oldAttributes != null) {
      var changed = false
      if (oldAttributes.libraryFile != newAttributes.libraryFile) {
        changed = true
        fsModel.moveFileAndReplace(this, oldAttributes.libraryFile, newAttributes.libraryFile)
      }
      if (oldAttributes.memberInfo != newAttributes.memberInfo) {
        changed = true
        fileToMemberInfoMap[file] = newAttributes.memberInfo
      }
      if (oldAttributes.contentMode != newAttributes.contentMode) {
        changed = true
        fileToContentTypeMap[file] = newAttributes.contentMode
      }
      if (changed) {
        sendTopic(
          topic = AttributesService.ATTRIBUTES_CHANGED,
          componentManager = dataOpsManager.componentManager
        ).onUpdate(oldAttributes, newAttributes, file)
      }
    }
  }

  override fun clearAttributes(file: MFVirtualFile) {
    val attributes = getAttributes(file) ?: return
    fileToMemberInfoMap.remove(file)
    fileToContentTypeMap.remove(file)
    sendTopic(
      topic = AttributesService.ATTRIBUTES_CHANGED,
      componentManager = dataOpsManager.componentManager
    ).onDelete(attributes, file)
  }

  override val attributesClass = RemoteMemberAttributes::class.java

  override val vFileClass = MFVirtualFile::class.java

}