package eu.ibagroup.formainframe.dataops.synchronizer.adapters

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class SeqDatasetContentAdapterFactory: MFContentAdapterFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): MFContentAdapter {
    return SeqDatasetContentAdapter(dataOpsManager)
  }
}

class SeqDatasetContentAdapter(
  dataOpsManager: DataOpsManager
): LReclContentAdapter<RemoteDatasetAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = RemoteDatasetAttributes::class.java

  override fun adaptContentToMainframe(content: ByteArray, attributes: RemoteDatasetAttributes): ByteArray {
    var lrecl = attributes.datasetInfo.recordLength ?: 80
    if (attributes.hasVariableFormatRecords()) {
      lrecl -= 4
    }
    if (attributes.hasVariablePrintFormatRecords()) {
      lrecl -= 1
    }
    return transferLinesByLRecl(content, lrecl)
  }

  override fun adaptContentFromMainframe(content: ByteArray, attributes: RemoteDatasetAttributes): ByteArray {
    if (attributes.hasVariablePrintFormatRecords()) {
      return removeFirstCharacter(content)
    }
    return content
  }
}
