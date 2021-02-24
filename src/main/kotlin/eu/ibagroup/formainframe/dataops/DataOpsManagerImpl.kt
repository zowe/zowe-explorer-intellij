package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.allocation.Allocator
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.content.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.AcceptancePolicy
import eu.ibagroup.formainframe.dataops.content.SaveStrategy
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.utils.findAnyNullable

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
    } as Allocator<R, Q>?
      ?: throw IllegalArgumentException(
        "Cannot find Allocator for queryClass=${queryClass.name} and requestClass=${requestClass.name}"
      )
  }

  override fun <A : VFileInfoAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>,
    vFileClass: Class<out F>
  ): AttributesService<A, F> {
    return attributesServices.find {
      it.attributesClass.isAssignableFrom(attributesClass) && it.vFileClass.isAssignableFrom(vFileClass)
    } as AttributesService<A, F>? ?: throw IllegalArgumentException(
      "Cannot find AttributesService for attributeClass=${attributesClass.name} and vFileClass=${vFileClass.name}"
    )
  }

  override fun tryToGetAttributes(file: VirtualFile): VFileInfoAttributes? {
    return attributesServices.stream()
      .map { it.getAttributes(file) }
      .filter { it != null }
      .findAnyNullable()
  }

  private val fileFetchProviders by lazy {
    FileFetchProvider.EP.extensionList
  }

  override fun <R : Any, Q : Query<R>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File> {
    return fileFetchProviders.find {
      it.requestClass.isAssignableFrom(requestClass)
          && it.queryClass.isAssignableFrom(queryClass)
          && it.vFileClass.isAssignableFrom(vFileClass)
    } as FileFetchProvider<R, Q, File>? ?: throw IllegalArgumentException(
      "Cannot find FileFetchProvider for " +
          "requestClass=${requestClass.name}; queryClass=${queryClass.name}; vFileClass=${vFileClass.name}"
    )
  }

  private val contentSynchronizers by lazy {
    ContentSynchronizer.EP.extensionList
  }

  private fun getAppropriateContentSynchronizer(file: VirtualFile): ContentSynchronizer {
    return contentSynchronizers.stream()
      .filter { it.accepts(file) }
      .findAnyNullable()
      ?: throw IllegalArgumentException("Cannot find appropriate ContentSynchronizer for ${file.path}")
  }

  override fun enforceContentSync(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy
  ) {
    getAppropriateContentSynchronizer(file).enforceSync(file, acceptancePolicy, saveStrategy)
  }

  override fun isContentSynced(file: VirtualFile): Boolean {
    return getAppropriateContentSynchronizer(file).isAlreadySynced(file)
  }

  override fun removeContentSync(file: VirtualFile) {
    getAppropriateContentSynchronizer(file).removeSync(file)
  }

  override fun dispose() {
    attributesServices.clear()
    allocators.clear()
    fileFetchProviders.clear()
    contentSynchronizers.clear()
  }

}