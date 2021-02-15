package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.sendTopic
import java.io.IOException

fun sendAttributesTopic(): AttributesListener = sendTopic(DataOpsManager.ATTRIBUTES_CHANGED)

interface AttributesService<Attributes : VFileInfoAttributes, VFile : VirtualFile> {

  companion object {
    @JvmStatic
    val EP = ExtensionPointName
      .create<AttributesService<VFileInfoAttributes, VirtualFile>>("eu.ibagroup.formainframe.attributesService")
  }

  @Throws(IOException::class)
  fun getOrCreateVirtualFile(attributes: Attributes): VFile

  fun getVirtualFile(attributes: Attributes): VFile?

//  @Throws(IOException::class)
//  fun getOrCreateVirtualFile(attributes: Attributes): VFile {
//    return getVirtualFile(attributes) ?: this.getOrCreateVirtualFile(attributes)
//  }

  fun getAttributes(file: VFile): Attributes?

  @Throws(IOException::class)
  fun updateAttributes(file: VFile, newAttributes: Attributes)

  @Suppress("UNCHECKED_CAST")
  @Throws(IOException::class)
  fun updateAttributes(file: VFile, updater: Attributes.() -> Unit) {
    getAttributes(file)?.let { updateAttributes(file, updater.cloneAndApply(it)) }
  }

  @Throws(IOException::class)
  fun updateAttributes(oldAttributes: Attributes, updater: Attributes.() -> Unit) {
    getVirtualFile(oldAttributes)?.let { updateAttributes(it, updater.cloneAndApply(oldAttributes)) }
  }

  @Throws(IOException::class)
  fun updateAttributes(oldAttributes: Attributes, newAttributes: Attributes) {
    getVirtualFile(oldAttributes)?.let { updateAttributes(it, newAttributes) }
  }

  @Throws(IOException::class)
  fun clearAttributes(file: VFile)

  @Throws(IOException::class)
  fun clearAttributes(attributes: Attributes) {
    getVirtualFile(attributes)?.let { clearAttributes(it) }
  }

  val attributesClass: Class<out Attributes>

  val vFileClass: Class<out VFile>

}

@Suppress("UNCHECKED_CAST")
fun <Attributes : VFileInfoAttributes> (Attributes.() -> Unit).cloneAndApply(attributes: Attributes): Attributes {
  val cloned = attributes.clone() as Attributes
  invoke(cloned)
  return cloned
}