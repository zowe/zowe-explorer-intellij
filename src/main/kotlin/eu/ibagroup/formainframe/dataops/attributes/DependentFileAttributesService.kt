package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.createAttributes
import eu.ibagroup.r2z.XIBMDataType
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap

abstract class DependentFileAttributesService<Attributes : DependentFileAttributes<InfoType, VFile>, InfoType, ParentAttributes: FileAttributes, VFile: VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : AttributesService<Attributes, VFile> {



  protected abstract val parentAttributesClass: Class<out ParentAttributes>

  private val remoteParentAttributesService by lazy {
    dataOpsManager.getAttributesService(parentAttributesClass, vFileClass)
  }

  private var fileToInfoMap = ConcurrentHashMap<VFile, InfoType>()
  private var fileToContentTypeMap = ConcurrentHashMap<VFile, XIBMDataType>()

  protected abstract fun buildAttributes(info: InfoType, file: VFile, contentMode: XIBMDataType?): Attributes

  protected abstract val findOrCreateFileInVFSModel:
        (Any?, VFile, String, com.intellij.openapi.util.io.FileAttributes) -> VFile

  protected abstract val moveFileAndReplaceInVFSModel: (requestor: Any?, vFile: VFile, newParent: VFile) -> Unit

  private fun getParent(parentFile: VFile): VFile? {
    val parentAttributes = runReadAction { remoteParentAttributesService.getAttributes(parentFile) }
    return if (parentAttributes != null) {
      parentFile
    } else null
  }

  protected fun getParentAttributes(childFile: VFile): ParentAttributes {
    val parent = childFile.parent.castOrNull(vFileClass)
    val attributes = if (parent != null) {
      remoteParentAttributesService.getAttributes(parent)
    } else null
    return attributes ?: throw IllegalArgumentException("Cannot find parent attributes")
  }

  override fun getOrCreateVirtualFile(attributes: Attributes): VFile {
    val parent = getParent(attributes.parentFile)
    return if (parent != null && parent.isDirectory) {
      findOrCreateFileInVFSModel(this, parent, attributes.name, createAttributes(directory = false)).also {
        fileToInfoMap[it] = attributes.info
        fileToContentTypeMap[it] = attributes.contentMode
        sendTopic(
          topic = AttributesService.ATTRIBUTES_CHANGED,
          componentManager = dataOpsManager.componentManager
        ).onCreate(attributes, it)
      }
    } else throw IOException("Cannot find child")
  }

  override fun getVirtualFile(attributes: Attributes): VFile? {
    val parent = getParent(attributes.parentFile)
    return parent?.findChild(attributes.name).castOrNull(vFileClass)
  }

  override fun getAttributes(file: VFile): Attributes? {
    val parent = file.parent?.castOrNull(vFileClass)?.let { getParent(it) }
    val info = fileToInfoMap[file]
    return if (parent != null && info != null) {
      buildAttributes(info, parent, fileToContentTypeMap[file])
    } else null
  }

  override fun updateAttributes(file: VFile, newAttributes: Attributes) {
    val oldAttributes = getAttributes(file)
    if (oldAttributes != null) {
      var changed = false
      if (oldAttributes.parentFile != newAttributes.parentFile) {
        changed = true
        moveFileAndReplaceInVFSModel(this, oldAttributes.parentFile, newAttributes.parentFile)
      }
      if(oldAttributes.info != newAttributes.info) {
        changed = true
        fileToInfoMap[file] = newAttributes.info
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

  override fun clearAttributes(file: VFile) {
    val attributes = getAttributes(file) ?: return
    fileToInfoMap.remove(file)
    fileToContentTypeMap.remove(file)
    sendTopic(
      topic = AttributesService.ATTRIBUTES_CHANGED,
      componentManager = dataOpsManager.componentManager
    ).onDelete(attributes, file)
  }
}