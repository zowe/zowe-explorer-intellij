package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import eu.ibagroup.formainframe.common.appLevelPluginDisposable
import eu.ibagroup.formainframe.dataops.dataOpsManager

class MFVirtualFileSystem : VirtualFileSystemModelWrapper<MFVirtualFile, MFVirtualFileSystemModel>(
  MFVirtualFile::class.java,
  MFVirtualFileSystemModel()
) {

  companion object {
    const val SEPARATOR = "/"
    const val PROTOCOL = "mf"
    const val ROOT_NAME = "For Mainframe"
    const val ROOT_ID = 0
    const val INVALID_FILE_PATH = "INVALID_FILE"

    @JvmStatic
    val instance
      get() = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as MFVirtualFileSystem

    @JvmStatic
    val model = instance.model
  }

  init {
    Disposer.register(appLevelPluginDisposable, this)
  }

  val root = model.root

  override fun isValidName(name: String) = name.isNotBlank()

}