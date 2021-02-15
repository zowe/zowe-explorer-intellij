package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.vfs.VirtualFileManager

class MFVirtualFileSystem : VirtualFileSystemModelWrapper<MFVirtualFile, MFVirtualFileSystemModel>(
  MFVirtualFile::class.java,
  MFVirtualFileSystemModel()
) {

  companion object {
    const val SEPARATOR = "/"
    const val PROTOCOL = "mf"
    const val ROOT_NAME = "For Mainframe"
    const val ROOT_ID = 0
    const val INVALID_ID = 0

    @JvmStatic
    val instance
      get() = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as MFVirtualFileSystem

    @JvmStatic
    val model = instance.model
  }

  val root = model.root

//  @Throws(IOException::class)
//  fun createChildWithAttributes(
//    requestor: Any?, vDir: MFVirtualFile, name: String, attributes: FileAttributes
//  ): MFVirtualFile = model.createChildWithAttributes(requestor, vDir, name, attributes)
//
//  @Throws(IOException::class)
//  fun findOrCreate(
//    requestor: Any?, vDir: MFVirtualFile, name: String, attributes: FileAttributes
//  ): MFVirtualFile {
//    var found = vDir.findChild(name)
//    if (found == null) {
//      found = createChildWithAttributes(requestor, vDir, name, attributes)
//    }
//    return found
//  }

  override fun isValidName(name: String) = name.isNotBlank()

}