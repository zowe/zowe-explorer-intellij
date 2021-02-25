package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

interface VirtualFileSystemModel<File : VirtualFile>: Disposable {

  fun getProtocol(): String

  fun getNioPath(file: File): Path? = null

  fun findFileByPath(path: String): File?

  fun refresh(asynchronous: Boolean)

  fun refreshAndFindFileByPath(path: String): File?

  fun addVirtualFileListener(listener: VirtualFileListener)

  fun removeVirtualFileListener(listener: VirtualFileListener)

  @Throws(IOException::class)
  fun deleteFile(requestor: Any?, vFile: File)

  @Throws(IOException::class)
  fun moveFile(requestor: Any?, vFile: File, newParent: File)

  @Throws(IOException::class)
  fun renameFile(requestor: Any?, vFile: File, newName: String)

  @Throws(IOException::class)
  fun createChildFile(requestor: Any?, vDir: File, fileName: String): File

  fun exists(file: File): Boolean

  fun list(file: File): Array<String>

  fun isDirectory(file: File): Boolean

  fun getTimeStamp(file: File): Long

  @Throws(IOException::class)
  fun setTimeStamp(file: File, timeStamp: Long)

  fun isWritable(file: File): Boolean

  @Throws(IOException::class)
  fun setWritable(file: File, writableFlag: Boolean)

  fun isSymLink(file: File): Boolean

  fun resolveSymLink(file: File): String?

  @Throws(IOException::class)
  fun createChildDirectory(requestor: Any?, vDir: File, dirName: String): File

  @Throws(IOException::class)
  fun copyFile(
    requestor: Any?,
    virtualFile: File,
    newParent: File,
    copyName: String
  ): File

  @Throws(IOException::class)
  fun contentsToByteArray(file: File): ByteArray

  @Throws(IOException::class)
  fun getInputStream(file: File): InputStream

  @Throws(IOException::class)
  fun getOutputStream(file: File, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream

  fun getLength(file: File): Long

  fun extractRootPath(normalizedPath: String) = MFVirtualFileSystem.SEPARATOR

  fun findFileByPathIfCached(path: String): File?

  fun getRank(): Int

  fun getAttributes(file: File): FileAttributes?

  fun isReadOnly(): Boolean

}