package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.allocation.Allocator
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.operations.Operation
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.utils.findAnyNullable

@Suppress("UNCHECKED_CAST")
class DataOpsManagerImpl : DataOpsManager {

  private fun <Component> List<DataOpsComponentFactory<Component>>.buildComponents(): MutableList<Component> {
    return map { it.buildComponent(this@DataOpsManagerImpl) }.toMutableSmartList()
  }

  override val componentManager: ComponentManager
    get() = ApplicationManager.getApplication()

  private val attributesServices by lazy {
    AttributesService.EP.extensionList.buildComponents() as MutableList<AttributesService<VFileInfoAttributes, VirtualFile>>
  }

  private val allocators = Allocator.EP.extensionList.buildComponents()

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

  override fun tryToGetFile(attributes: VFileInfoAttributes): VirtualFile? {
    return attributesServices.stream()
      .map { it.getVirtualFile(attributes) }
      .filter { it != null }
      .findAnyNullable()
  }

  private val fileFetchProviders by lazy {
    FileFetchProvider.EP.extensionList.buildComponents()
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
    ContentSynchronizer.EP.extensionList.buildComponents()
  }

  override fun getAppropriateContentSynchronizer(file: VirtualFile): ContentSynchronizer {
    return contentSynchronizers.stream()
      .filter { it.accepts(file) }
      .findAnyNullable()
      ?: throw IllegalArgumentException("Cannot find appropriate ContentSynchronizer for ${file.path}")
  }

  private val operationRunners by lazy {
    OperationRunner.EP.extensionList.buildComponents() as MutableList<OperationRunner<Operation>>
  }

  override fun isOperationSupported(operation: Operation): Boolean {
    return operationRunners.any { it.canRun(operation) }
  }

  override fun performOperation(operation: Operation, callback: FetchCallback<Unit>, project: Project?) {
    operationRunners.stream()
      .filter { it.canRun(operation) }
      .findAnyNullable()?.run(operation, callback, project)
      ?: throw IllegalArgumentException("Unsupported Operation $operation")
  }

  override fun dispose() {
    attributesServices.clear()
    allocators.clear()
    fileFetchProviders.clear()
    contentSynchronizers.clear()
  }
}