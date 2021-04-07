package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import java.io.IOException

interface AttributesService<Attributes : VFileInfoAttributes, VFile : VirtualFile> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<AttributesServiceFactory>("eu.ibagroup.formainframe.attributesService")

    @JvmField
    val ATTRIBUTES_CHANGED = Topic.create("attributesChanged", AttributesListener::class.java)
  }

  @Throws(IOException::class)
  fun getOrCreateVirtualFile(attributes: Attributes): VFile

  fun getVirtualFile(attributes: Attributes): VFile?

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