package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes

val dataOpsManager
  get() = DataOpsManager.instance

class DataOpsManagerImpl : DataOpsManager {

  private val attributesServices = AttributesService.EP.extensionList

  override fun <A : VFileInfoAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>,
    vFileClass: Class<out F>
  ): AttributesService<A, F> {
    @Suppress("UNCHECKED_CAST")
    return attributesServices.find {
      attributesClass.isAssignableFrom(it.attributesClass) && vFileClass.isAssignableFrom(it.vFileClass)
    } as AttributesService<A, F>? ?: throw IllegalArgumentException(
      "AttributeService for attributeClass=${attributesClass.name} and vFileClass=${vFileClass.name} is not registered"
    )
  }

  override fun tryToGetAttributes(file: VirtualFile): VFileInfoAttributes? {
    return attributesServices.mapNotNull { it.getAttributes(file) }.firstOrNull()
  }

}