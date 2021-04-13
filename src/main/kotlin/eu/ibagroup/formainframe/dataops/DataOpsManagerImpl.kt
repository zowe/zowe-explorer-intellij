package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.utils.findAnyNullable

@Suppress("UNCHECKED_CAST")
class DataOpsManagerImpl : DataOpsManager {

  private fun <Component> List<DataOpsComponentFactory<Component>>.buildComponents(): MutableList<Component> {
    return buildComponents(this@DataOpsManagerImpl)
  }

  override val componentManager: ComponentManager
    get() = ApplicationManager.getApplication()

  private val attributesServices by lazy {
    AttributesService.EP.extensionList.buildComponents() as MutableList<AttributesService<VFileInfoAttributes, VirtualFile>>
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
      .filter { it.vFileClass.isAssignableFrom(file::class.java) }
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

  override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*, *>>,
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

  override fun isSyncSupported(file: VirtualFile): Boolean {
    return contentSynchronizers.stream()
      .filter { it.accepts(file) }
      .findAnyNullable() != null
  }

  override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
    return contentSynchronizers.stream()
      .filter { it.accepts(file) }
      .findAnyNullable()
  }

  private val operationRunners by lazy {
    OperationRunner.EP.extensionList.buildComponents() as MutableList<OperationRunner<Operation<*>, *>>
  }

  override fun isOperationSupported(operation: Operation<*>): Boolean {
    return operationRunners.any {
      if (it.operationClass.isAssignableFrom(operation::class.java) && it.resultClass.isAssignableFrom(operation.resultClass)) {
        it.canRun(operation)
      } else {
        false
      }
    }
  }

  override fun <R : Any> performOperation(
    operation: Operation<R>,
    progressIndicator: ProgressIndicator
  ): R {
    return (operationRunners.stream()
      .filter {
        if (it.operationClass.isAssignableFrom(operation::class.java) && it.resultClass.isAssignableFrom(operation.resultClass)) {
          (it as OperationRunner<Operation<R>, R>).canRun(operation)
        } else {
          false
        }
      }
      .findAnyNullable() as OperationRunner<Operation<R>, R>?)
      ?.run(operation, progressIndicator)
      ?: throw IllegalArgumentException("Unsupported Operation $operation")
  }

  override fun dispose() {
    attributesServices.clear()
    fileFetchProviders.clear()
    contentSynchronizers.clear()
  }
}