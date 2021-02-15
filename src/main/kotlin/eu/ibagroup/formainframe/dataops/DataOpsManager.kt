package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.attributes.AttributesListener
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes

interface DataOpsManager {

  companion object {
    @JvmStatic
    val ATTRIBUTES_CHANGED = Topic.create("attributesChanged", AttributesListener::class.java)

    @JvmStatic
    val instance: DataOpsManager
      get() = ApplicationManager.getApplication().getService(DataOpsManager::class.java)
  }

  fun <A : VFileInfoAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>, vFileClass: Class<out F>
  ) : AttributesService<A, F>

  fun tryToGetAttributes(file: VirtualFile): VFileInfoAttributes?

}

inline fun <reified A : VFileInfoAttributes, reified F : VirtualFile> DataOpsManager.getAttributesService(): AttributesService<A, F> {
  return getAttributesService(A::class.java, F::class.java)
}