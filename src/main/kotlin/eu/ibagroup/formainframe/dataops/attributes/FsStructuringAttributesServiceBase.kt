package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.sendTopic
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

abstract class FsStructuringAttributesServiceBase<Attributes : VFileInfoAttributes, VFile : VirtualFile>(
  private val dataOpsManager: DataOpsManager
) : AttributesService<Attributes, VFile> {

  abstract val subFolderName: String

  private val lock = ReentrantReadWriteLock()

  private val fileToAttributesMap = HashMap<VFile, Attributes>()
  private val attributesToFileMap = HashMap<Attributes, VFile>()

  override fun getOrCreateVirtualFile(attributes: Attributes): VFile {
    return lock.write {
      getVirtualFile(attributes)?.let {
        if (!it.isValid) return@let
        val oldAttributes = getAttributes(it)
        if (oldAttributes != null) {
          updateAttributesInternal(it, oldAttributes, mergeAttributes(oldAttributes, attributes))
          return it
        }
      }

      val createdFile = findOrCreateFileInternal(attributes)
      fileToAttributesMap[createdFile] = attributes
      attributesToFileMap[buildUniqueAttributes(attributes)] = createdFile
      sendTopic(AttributesService.ATTRIBUTES_CHANGED).onCreate(attributes, createdFile)

      createdFile
    }
  }

  protected abstract fun buildUniqueAttributes(attributes: Attributes): Attributes

  protected abstract fun findOrCreateFileInternal(attributes: Attributes): VFile

  private fun updateAttributesInternal(
    file: VFile,
    oldAttributes: Attributes,
    newAttributes: Attributes
  ) {
    lock.write {
      attributesToFileMap.remove(buildUniqueAttributes(oldAttributes))
      reassignAttributesToFile(file, oldAttributes, newAttributes)
      fileToAttributesMap[file] = newAttributes
      attributesToFileMap[buildUniqueAttributes(newAttributes)] = file
      sendTopic(AttributesService.ATTRIBUTES_CHANGED, dataOpsManager.componentManager)
        .onUpdate(oldAttributes, newAttributes, file)
    }
  }

  protected abstract fun reassignAttributesToFile(
    file: VFile, oldAttributes: Attributes, newAttributes: Attributes
  )

  @Throws(IOException::class)
  protected abstract fun mergeAttributes(oldAttributes: Attributes, newAttributes: Attributes): Attributes

  override fun getVirtualFile(attributes: Attributes): VFile? {
    return lock.read { attributesToFileMap[buildUniqueAttributes(attributes)] }
  }

  fun getVirtualFileExactly(attributes: Attributes): VFile? {
    lock.read {
      val found = getVirtualFile(attributes)
      return if (found != null && getAttributes(found) == attributes) {
        found
      } else null
    }
  }

  override fun getAttributes(file: VFile): Attributes? {
    return lock.read { fileToAttributesMap[file] }
  }

  override fun updateAttributes(file: VFile, newAttributes: Attributes) {
    lock.write { getAttributes(file)?.let { updateAttributesInternal(file, it, newAttributes) } }
  }

  override fun clearAttributes(file: VFile) {
    lock.write {
      getAttributes(file)?.let {
        attributesToFileMap.remove(it)
        fileToAttributesMap.remove(file)
        sendTopic(AttributesService.ATTRIBUTES_CHANGED, dataOpsManager.componentManager).onDelete(it, file)
      }
    }
  }

}