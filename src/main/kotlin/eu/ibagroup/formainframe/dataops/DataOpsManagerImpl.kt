package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.allocation.Allocator
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider

@Suppress("UNCHECKED_CAST")
class DataOpsManagerImpl : DataOpsManager {

  private val attributesServices = AttributesService.EP.extensionList

  private val allocators = Allocator.EP.extensionList

  override fun <R : Any, Q : Query<R>> getAllocator(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*>>
  ): Allocator<R, Q> {
    return allocators.find {
      it.requestClass.isAssignableFrom(requestClass) && it.queryClass.isAssignableFrom(queryClass)
    } as Allocator<R, Q>? ?: throw IllegalArgumentException("Cannot find allocator for Query: $queryClass and Request: $requestClass")
  }

  override fun <A : VFileInfoAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>,
    vFileClass: Class<out F>
  ): AttributesService<A, F> {
    return attributesServices.find {
      it.attributesClass.isAssignableFrom(attributesClass) && it.vFileClass.isAssignableFrom(vFileClass)
    } as AttributesService<A, F>? ?: throw IllegalArgumentException(
      "AttributeService for attributeClass=${attributesClass.name} and vFileClass=${vFileClass.name} is not registered"
    )
  }

  override fun tryToGetAttributes(file: VirtualFile): VFileInfoAttributes? {
    return attributesServices.mapNotNull { it.getAttributes(file) }.firstOrNull()
  }

  private val fileFetchProviders by lazy {
    FileFetchProvider.EP.extensionList
  }

  @Suppress("UNCHECKED_CAST")
  override fun <R : Any, Q : Query<R>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File> {
    return fileFetchProviders.find {
      it.requestClass.isAssignableFrom(requestClass)
          && it.queryClass.isAssignableFrom(queryClass)
          && it.vFileClass.isAssignableFrom(vFileClass)
    } as FileFetchProvider<R, Q, File>? ?: throw Exception("TODO File Fetch Provider not registered")
  }

}