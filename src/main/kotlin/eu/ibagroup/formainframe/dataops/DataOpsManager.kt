package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.allocation.Allocator
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.operations.Operation

interface DataOpsManager : Disposable {

  companion object {
    @JvmStatic
    val instance: DataOpsManager
      get() = ApplicationManager.getApplication().getService(DataOpsManager::class.java)
  }

  fun <R : Any, Q : Query<R>> getAllocator(requestClass: Class<out R>, queryClass: Class<out Query<*>>): Allocator<R, Q>

  fun <A : VFileInfoAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>, vFileClass: Class<out F>
  ): AttributesService<A, F>

  fun tryToGetAttributes(file: VirtualFile): VFileInfoAttributes?

  fun tryToGetFile(attributes: VFileInfoAttributes): VirtualFile?

  fun <R : Any, Q : Query<R>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File>

  fun getAppropriateContentSynchronizer(file: VirtualFile): ContentSynchronizer

  fun isOperationSupported(operation: Operation): Boolean

  fun performOperation(operation: Operation, callback: FetchCallback<Unit>, project: Project? = null)

  val componentManager: ComponentManager

}

inline fun <reified A : VFileInfoAttributes, reified F : VirtualFile> DataOpsManager.getAttributesService(): AttributesService<A, F> {
  return getAttributesService(A::class.java, F::class.java)
}