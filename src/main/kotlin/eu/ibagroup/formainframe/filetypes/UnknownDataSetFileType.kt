package eu.ibagroup.formainframe.filetypes

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import javax.swing.Icon

class UnknownDataSetFileType : FileTypeIdentifiableByVirtualFile {
  override fun getName(): String {
    return "Unknown Data Set"
  }

  override fun getDescription(): String {
    return "Unknown data set type"
  }

  override fun getDefaultExtension(): String {
    return ""
  }

  override fun getIcon(): Icon {
    return AllIcons.FileTypes.Text
  }

  override fun isBinary(): Boolean {
    return false
  }

  override fun getCharset(file: VirtualFile, content: ByteArray?): String {
    return file.charset.name()
  }

  override fun isMyFileType(file: VirtualFile): Boolean {
    return false
//    val attributes = service<DataOpsManager>().tryToGetAttributes(file)
//    return (attributes is RemoteDatasetAttributes || attributes is RemoteMemberAttributes)
//      && file.fi
  }
}