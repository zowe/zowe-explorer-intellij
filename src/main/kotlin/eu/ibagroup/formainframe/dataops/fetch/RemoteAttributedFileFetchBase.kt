package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.*
import retrofit2.Response

abstract class RemoteAttributedFileFetchBase<Request : Any, Response : FileAttributes, File : VirtualFile>(
  dataOpsManager: DataOpsManager,
  val BATCH_SIZE: Int = 100
) : RemoteFileFetchProviderBase<Request, Response, File>(dataOpsManager) {

  protected val attributesService: AttributesService<Response, File>
      by lazy { dataOpsManager.getAttributesService(responseClass, vFileClass) }

  override fun convertResponseToFile(response: Response): File? {
    return attributesService.getOrCreateVirtualFile(response)
  }
}
