/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.FileSystemInterface
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.DataOpsManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * A listener for VFS events of [MFVirtualFileSystem].
 * @see BulkFileListener
 * @author Valiantsin Krus
 */
interface MFBulkFileListener {
  /**
   * Handler for events preprocessing.
   * @param events list of events that will be processed soon in [MFVirtualFileSystem].
   */
  fun before(events: List<VFileEvent>) {}

  /**
   * Handler for events postprocessing.
   * @param events list of events that have been recently processed in [MFVirtualFileSystem].
   */
  fun after(events: List<VFileEvent>) {}
}


/** Class that implements all the functionality of the mainframe virtual file system */
class MFVirtualFileSystem : VirtualFileSystem(), FileSystemInterface, Disposable {

  val model = MFVirtualFileSystemModel()
  private val vFileClass = MFVirtualFile::class.java
  val root = model.root

  companion object {
    const val SEPARATOR = "/"
    const val PROTOCOL = "mf"
    const val ROOT_NAME = "For Mainframe"
    const val ROOT_ID = 0
    private val custMigrVols = listOf("ARCIVE")

    val MF_VFS_CHANGES_TOPIC = Topic.create("mfVfsChanges", MFBulkFileListener::class.java)

    @JvmStatic
    val instance: MFVirtualFileSystem
      get() = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as MFVirtualFileSystem

    /**
     * Check if the provided volume belongs to custom migration volumes list
     * @param vol the volume to check
     * @return true if belongs, false otherwise
     */
    @JvmStatic
    fun belongsToCustMigrVols(vol: String): Boolean {
      return custMigrVols.contains(vol)
    }
  }

  init {
    Disposer.register(DataOpsManager.getService(), this)
  }

  private val onIncorrectFileTypeException: (VirtualFile) -> RuntimeException = {
    UnsupportedOperationException(
      "${it.path} is not supported by this FS. Only instances of ${vFileClass.name} are allowed"
    )
  }

  /**
   * Check if the file is our instance
   * @param callback callback to call when the file is our instance
   */
  private inline fun <R> VirtualFile.ifOurInstance(
    callback: (MFVirtualFile) -> R
  ): R {
    return if (vFileClass.isAssignableFrom(this::class.java)) {
      @Suppress("UNCHECKED_CAST")
      callback(this as MFVirtualFile)
    } else {
      throw onIncorrectFileTypeException(this)
    }
  }

  override fun isValidName(name: String) = name.isNotBlank()

  override fun getProtocol(): String {
    return model.getProtocol()
  }

  override fun findFileByPath(path: String): MFVirtualFile? {
    return model.findFileByPath(path)
  }

  override fun refresh(asynchronous: Boolean) {
    return model.refresh()
  }

  override fun refreshAndFindFileByPath(path: String): MFVirtualFile? {
    return model.refreshAndFindFileByPath(path)
  }

  override fun addVirtualFileListener(listener: VirtualFileListener) {
    return model.addVirtualFileListener()
  }

  override fun removeVirtualFileListener(listener: VirtualFileListener) {
    return model.removeVirtualFileListener()
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
  override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): MFVirtualFile {
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
  override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): MFVirtualFile {
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
  ): MFVirtualFile {
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

  override fun dispose() {
    model.dispose()
  }

}
