package eu.ibagroup.formainframe.dataops.content.adapters

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class DefaultContentAdapter(dataOpsManager: DataOpsManager): MFContentAdapterBase<FileAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = FileAttributes::class.java

  override fun adaptContentToMainframe(content: ByteArray, attributes: FileAttributes): ByteArray = content
  override fun adaptContentFromMainframe(content: ByteArray, attributes: FileAttributes): ByteArray = content
}
