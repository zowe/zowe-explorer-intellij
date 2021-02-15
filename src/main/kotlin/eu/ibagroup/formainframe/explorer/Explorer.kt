package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.fetch.Query
import eu.ibagroup.formainframe.dataops.fetch.RemoteQuery
import eu.ibagroup.formainframe.vfs.MFVirtualFile

val globalExplorer
  get() = Explorer.appInstance

interface Explorer {

  companion object {
    @JvmStatic
    val appInstance: Explorer
      get() = ApplicationManager.getApplication().getService(Explorer::class.java)
  }

  val project: Project?

  val units: Collection<ExplorerUnit>

  val dataOpsManager: DataOpsManager

  fun <R : Any, Q : Query<R>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File>

}

inline fun <reified R : Any, reified Q : Query<R>, reified File : VirtualFile> Explorer.getFileFetchProvider(): FileFetchProvider<R, Q, File> {
  return getFileFetchProvider(R::class.java, Q::class.java, File::class.java)
}

inline fun <reified R : Any, reified Q : Query<R>> Explorer.getMfFileFetchProvider(): FileFetchProvider<R, Q, MFVirtualFile> {
  return getFileFetchProvider(R::class.java, Q::class.java, MFVirtualFile::class.java)
}

inline fun <reified R : Any> Explorer.getRemoteMfFileFetchProvider(): FileFetchProvider<R, RemoteQuery<R>, MFVirtualFile> {
  return getFileFetchProvider()
}