package eu.ibagroup.formainframe.dataops.synchronizer.adapters

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class MemberContentAdapterFactory: MFContentAdapterFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MFContentAdapter {
    return MemberContentAdapter(dataOpsManager)
  }
}

class MemberContentAdapter(
  dataOpsManager: DataOpsManager
): LReclContentAdapter<RemoteMemberAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = RemoteMemberAttributes::class.java

  override fun adaptContentToMainframe(content: ByteArray, attributes: RemoteMemberAttributes): ByteArray {
    val pdsAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
    var lrecl = pdsAttributes.datasetInfo.recordLength ?: 80
    if (pdsAttributes.hasVariableFormatRecords()) {
      lrecl -= 4
    }
    if (pdsAttributes.hasVariablePrintFormatRecords()) {
      lrecl -= 1
    }
    return transferLinesByLRecl(content, lrecl)
  }

  override fun adaptContentFromMainframe(content: ByteArray, attributes: RemoteMemberAttributes): ByteArray {
    val pdsAttributes = dataOpsManager.tryToGetAttributes(attributes.parentFile) as RemoteDatasetAttributes
    if (pdsAttributes.hasVariablePrintFormatRecords()) {
      return removeFirstCharacter(content)
    }
    return content
  }

}
