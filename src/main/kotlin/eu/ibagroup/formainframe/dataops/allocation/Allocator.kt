package eu.ibagroup.formainframe.dataops.allocation

import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.FetchCallback
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider

interface Allocator<R : Any, Q: Query<R>> {

  companion object {
    @JvmStatic
    val EP = ExtensionPointName.create<AllocatorFactory>("eu.ibagroup.formainframe.allocator")
  }

  fun allocate(query: Q, callback: FetchCallback<AllocationStatus>, project: Project? = null)

  val requestClass: Class<out R>

  val queryClass: Class<out Query<*>>

}

enum class AllocationStatus {
  SUCCESS,
  FAILED
}