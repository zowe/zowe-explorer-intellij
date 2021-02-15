package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.explorer.Explorer

abstract class RemoteAttributedFileFetchBase<Request : Any, Response : VFileInfoAttributes, File : VirtualFile>(
  explorer: Explorer
) : RemoteFileFetchProviderBase<Request, Response, File>(explorer) {

  override fun convertResponseToFile(response: Response): File {
    return explorer.dataOpsManager.getAttributesService(responseClass, vFileClass)
      .getOrCreateVirtualFile(response)
//    val lock = ReentrantLock()
//    val condition = lock.newCondition()
//    var file: File? = null
//    @Suppress("UnstableApiUsage")
//    ApplicationManager.getApplication().invokeLaterOnWriteThread {
//      lock(lock) {
//        file = explorer.dataOpsManager.getAttributesService(responseClass, vFileClass)
//          .getOrCreateVirtualFile(response)
//        condition.signalAll()
//      }
//    }
//    lock(lock) { condition.await() }
//    return file ?: throw RuntimeException("Failed to convert response to file")
  }

}