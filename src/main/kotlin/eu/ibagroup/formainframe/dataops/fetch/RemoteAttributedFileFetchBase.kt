package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.dataOpsManager

abstract class RemoteAttributedFileFetchBase<Request : Any, Response : VFileInfoAttributes, File : VirtualFile>
  : RemoteFileFetchProviderBase<Request, Response, File>() {

  protected val attributesService: AttributesService<Response, File>
      by lazy { dataOpsManager.getAttributesService(responseClass, vFileClass) }

  override fun convertResponseToFile(response: Response): File {
    return attributesService.getOrCreateVirtualFile(response)
  }

}