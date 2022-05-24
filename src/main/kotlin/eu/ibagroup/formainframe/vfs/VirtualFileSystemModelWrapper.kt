package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.FileSystemInterface
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

open class VirtualFileSystemModelWrapper<File : VirtualFile, Model : VirtualFileSystemModel<File>>(
  private val vFileClass: Class<out File>,
  val model: Model,
  private val onIncorrectFileTypeException: (VirtualFile) -> RuntimeException = {
    UnsupportedOperationException(
      "${it.path} is not supported by this FS. Only instances of ${vFileClass.name} are allowed"
    )
  }
) : VirtualFileSystem(), FileSystemInterface, Disposable by model {

  private inline fun <R> VirtualFile.ifOurInstance(
    onOurBlock: (File) -> R
  ): R {
    return if (vFileClass.isAssignableFrom(this::class.java)) {
      @Suppress("UNCHECKED_CAST")
      onOurBlock(this as File)
    } else {
      throw onIncorrectFileTypeException(this)
    }
  }

  override fun getProtocol(): String {
    return model.getProtocol()
  }

//  override fun getNioPath(file: VirtualFile): Path? {
//    return file.ifOurInstance { model.getNioPath(it) }
//  }

  override fun findFileByPath(path: String): File? {
    return model.findFileByPath(path)
  }

  override fun refresh(asynchronous: Boolean) {
    return model.refresh(asynchronous)
  }

  override fun refreshAndFindFileByPath(path: String): File? {
    return model.refreshAndFindFileByPath(path)
  }

  override fun addVirtualFileListener(listener: VirtualFileListener) {
    return model.addVirtualFileListener(listener)
  }

  override fun removeVirtualFileListener(listener: VirtualFileListener) {
    return model.removeVirtualFileListener(listener)
  }

  @Throws(IOException::class)
  override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
    vFile.ifOurInstance { model.deleteFile(requestor, it) }
  }

  @Throws(IOException::class)
  override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
    vFile.ifOurInstance { file ->
      newParent.ifOurInstance { new ->
        model.moveFile(requestor, file, new)
      }
    }
  }

  @Throws(IOException::class)
  override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
    vFile.ifOurInstance { model.renameFile(requestor, it, newName) }
  }

  @Throws(IOException::class)
  override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): File {
    return vDir.ifOurInstance {
      model.createChildFile(requestor, it, fileName)
    }
  }

  override fun exists(file: VirtualFile): Boolean {
    return file.ifOurInstance {
      model.exists(it)
    }
  }

  override fun list(file: VirtualFile): Array<String> {
    return file.ifOurInstance {
      model.list(it)
    }
  }

  override fun isDirectory(file: VirtualFile): Boolean {
    return file.ifOurInstance {
      model.isDirectory(it)
    }
  }

  override fun getTimeStamp(file: VirtualFile): Long {
    return file.ifOurInstance {
      model.getTimeStamp(it)
    }
  }

  @Throws(IOException::class)
  override fun setTimeStamp(file: VirtualFile, timeStamp: Long) {
    return file.ifOurInstance {
      model.setTimeStamp(it, timeStamp)
    }
  }

  override fun isWritable(file: VirtualFile): Boolean {
    return file.ifOurInstance {
      model.isWritable(it)
    }
  }

  @Throws(IOException::class)
  override fun setWritable(file: VirtualFile, writableFlag: Boolean) {
    return file.ifOurInstance {
      model.setWritable(it, writableFlag)
    }
  }

  override fun isSymLink(file: VirtualFile): Boolean {
    return file.ifOurInstance {
      model.isSymLink(it)
    }
  }

  override fun resolveSymLink(file: VirtualFile): String? {
    return file.ifOurInstance {
      model.resolveSymLink(it)
    }
  }

  @Throws(IOException::class)
  override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): File {
    return vDir.ifOurInstance {
      model.createChildDirectory(requestor, it, dirName)
    }
  }

  @Throws(IOException::class)
  override fun copyFile(
    requestor: Any?,
    virtualFile: VirtualFile,
    newParent: VirtualFile,
    copyName: String
  ): File {
    return virtualFile.ifOurInstance { file ->
      newParent.ifOurInstance { parent ->
        model.copyFile(requestor, file, parent, copyName)
      }
    }
  }

  @Throws(IOException::class)
  override fun contentsToByteArray(file: VirtualFile): ByteArray {
    return file.ifOurInstance {
      model.contentsToByteArray(it)
    }
  }

  @Throws(IOException::class)
  override fun getInputStream(file: VirtualFile): InputStream {
    return file.ifOurInstance {
      model.getInputStream(it)
    }
  }

  @Throws(IOException::class)
  override fun getOutputStream(file: VirtualFile, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream {
    return file.ifOurInstance {
      model.getOutputStream(it, requestor, modStamp, timeStamp)
    }
  }

  override fun getLength(file: VirtualFile): Long {
    return file.ifOurInstance {
      model.getLength(it)
    }
  }

  fun extractRootPath(normalizedPath: String): String {
    return model.extractRootPath(normalizedPath)
  }

  fun findFileByPathIfCached(path: String): VirtualFile? {
    return model.findFileByPathIfCached(path)
  }

  fun getRank(): Int {
    return model.getRank()
  }

  fun getAttributes(file: VirtualFile): FileAttributes? {
    return file.ifOurInstance {
      model.getAttributes(it)
    }
  }

  override fun isReadOnly(): Boolean {
    return model.isReadOnly()
  }
}