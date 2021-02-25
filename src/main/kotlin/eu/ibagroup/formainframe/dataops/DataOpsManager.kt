package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.allocation.Allocator
import eu.ibagroup.formainframe.dataops.attributes.AttributesListener
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.content.AcceptancePolicy
import eu.ibagroup.formainframe.dataops.content.SaveStrategy
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import java.io.IOException

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

  fun <R : Any, Q : Query<R>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File>

  @Throws(IOException::class)
  fun enforceContentSync(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy
  )

  fun isContentSynced(file: VirtualFile): Boolean

  @Throws(IOException::class)
  fun syncContentIfNeeded(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy
  ) {
    if (!isContentSynced(file)) {
      enforceContentSync(file, acceptancePolicy, saveStrategy)
    }
  }

  fun removeContentSync(file: VirtualFile)

  val componentManager: ComponentManager

}

inline fun <reified A : VFileInfoAttributes, reified F : VirtualFile> DataOpsManager.getAttributesService(): AttributesService<A, F> {
  return getAttributesService(A::class.java, F::class.java)
}