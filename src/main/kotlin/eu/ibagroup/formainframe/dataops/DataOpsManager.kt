package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.synchronizer.ContentSynchronizer

interface DataOpsManager : Disposable {

  companion object {
    @JvmStatic
    val instance: DataOpsManager
      get() = ApplicationManager.getApplication().getService(DataOpsManager::class.java)
  }

  fun <A : VFileInfoAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>, vFileClass: Class<out F>
  ): AttributesService<A, F>

  fun tryToGetAttributes(file: VirtualFile): VFileInfoAttributes?

  fun tryToGetFile(attributes: VFileInfoAttributes): VirtualFile?

  fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*, *>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File>

  fun isSyncSupported(file: VirtualFile): Boolean

  fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer?

  fun isOperationSupported(operation: Operation<*>): Boolean

  @Throws(Throwable::class)
  fun <R : Any> performOperation(
    operation: Operation<R>,
    progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE
  ): R

  val componentManager: ComponentManager

}

inline fun <reified A : VFileInfoAttributes, reified F : VirtualFile> DataOpsManager.getAttributesService(): AttributesService<A, F> {
  return getAttributesService(A::class.java, F::class.java)
}

fun <C> List<DataOpsComponentFactory<C>>.buildComponents(dataOpsManager: DataOpsManager): MutableList<C> {
  return map { it.buildComponent(dataOpsManager) }.toMutableSmartList()
}