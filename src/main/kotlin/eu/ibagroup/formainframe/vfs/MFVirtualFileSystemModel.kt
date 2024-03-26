/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.ByteArraySequence
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jgrapht.Graph
import org.jgrapht.Graphs
import org.jgrapht.graph.DirectedMultigraph
import java.io.*
import java.nio.file.FileAlreadyExistsException
import java.nio.file.InvalidPathException
import java.nio.file.NotDirectoryException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition

private val log = logger<MFVirtualFileSystemModel>()

class FsOperationException(operationName: String, file: MFVirtualFile) : IOException(
  "Cannot perform $operationName on ${file.path}"
)

internal fun sendVfsChangesTopic() = sendTopic(VirtualFileManager.VFS_CHANGES)

internal fun sendMFVfsChangesTopic() = sendTopic(MFVirtualFileSystem.MF_VFS_CHANGES_TOPIC)

enum class FSEdgeType {
  DIR, SOFT_LINK, HARD_LINK;

  fun isSymlink(): Boolean {
    return this == SOFT_LINK || this == HARD_LINK
  }
}

private const val VFS_CONTENT_STORAGE_NAME = "mf_vfs_contents"

class FSEdge @JvmOverloads constructor(val type: FSEdgeType = FSEdgeType.DIR)

/** Interface to describe main functionalities for the mainframe virtual file system model */
class MFVirtualFileSystemModel {

  private var isDisposed = false

  companion object {
    private val counter = AtomicInteger(MFVirtualFileSystem.ROOT_ID)
  }

  private val fsGraph = AsConcurrentGraph(
    DirectedMultigraph<MFVirtualFile, FSEdge>(FSEdge::class.java)
  )

  private val contentStorage = ContentStorage(VFS_CONTENT_STORAGE_NAME)

  private val fileIdToStorageIdMap = ConcurrentHashMap<Int, Int>()

  private val initialContentConditions = ConcurrentHashMap<MFVirtualFile, Condition>()

  val root = MFVirtualFile(
    generateId(),
    MFVirtualFileSystem.ROOT_NAME,
    createAttributes(
      directory = true,
      special = false,
      symlink = false,
      hidden = false,
      length = 0,
      lastModified = System.nanoTime(),
      writable = false
    )
  )
    .apply {
      if (fsGraph.addVertex(this)) {
        isValidInternal = true
      }
    }

  private fun generateId() = counter.getAndIncrement()

  fun getProtocol() = MFVirtualFileSystem.PROTOCOL

  /**
   * Find or create the child in directory by its name
   * @param requestor the class to describe event requester
   * @param vDir the virtual directory to search in
   * @param name the name of the child
   * @param attributes the attributes to create the child if it is not found
   */
  @Throws(IOException::class)
  fun findOrCreate(
    requestor: Any?, vDir: MFVirtualFile, name: String, attributes: FileAttributes
  ): MFVirtualFile {
    return vDir.findChild(name) ?: createChildWithAttributes(requestor, vDir, name, attributes, false)
  }

  /**
   * Find or create the dependent file in directory (member or spool file)
   * by its name for member and by its name and id for spool file
   * @param requestor the class to describe event requester
   * @param vDir the virtual directory to search in
   * @param remoteAttributes attributes from the remote for the file to be found or created
   * @param attributes the attributes to create the child if it is not found
   */
  fun findOrCreateDependentFile(
    requestor: Any?,
    vDir: MFVirtualFile,
    remoteAttributes: eu.ibagroup.formainframe.dataops.attributes.FileAttributes,
    attributes: FileAttributes
  ): MFVirtualFile {
    return vDir.findChild(remoteAttributes.name)?.let { vFile ->
      remoteAttributes.castOrNull<RemoteSpoolFileAttributes>()?.let {
        val existingAttributes = service<DataOpsManager>().tryToGetAttributes(vFile) as RemoteSpoolFileAttributes
        if (existingAttributes.info.id != it.info.id) {
          createChildWithAttributes(requestor, vDir, remoteAttributes.name, attributes, true)
        } else {
          vFile
        }
      } ?: vFile
    } ?: createChildWithAttributes(requestor, vDir, remoteAttributes.name, attributes, false)
  }

  /**
   * Find the mainframe virtual file by provided path elements list
   * @param pathElements list of path elements to search for virtual file
   */
  fun findFileByPath(pathElements: List<String>): MFVirtualFile? {
    var pointerIndex = 1
    return FilteringBFSIterator(fsGraph, root) { v, e ->
      v.validReadLock(false) {
        (pointerIndex < pathElements.size
            && e.type == FSEdgeType.DIR
            && pathElements[pointerIndex] == v.name)
          .also { successful ->
            if (successful) ++pointerIndex
          }
      }
    }.stream().skip(pathElements.size - 1L).findAny().nullable
  }

  /**
   * Find the mainframe virtual file by provided path. Usually, the path is a URL string with "/" at the end,
   * so the filter is used to remove empty element at the end
   * @param path string path to search for the virtual file
   */
  fun findFileByPath(path: String): MFVirtualFile? {
    val pathElements = path.formatPath().split(MFVirtualFileSystem.SEPARATOR).filter(String::isEmpty)
    return findFileByPath(pathElements)
  }

  fun refresh() {
  }

  /**
   * Find file by path or refresh the virtual file system model and try to find the file again
   * @param path string path to search for the virtual file
   */
  fun refreshAndFindFileByPath(path: String): MFVirtualFile? {
    return findFileByPath(path).let {
      if (it == null) {
        refresh()
        return findFileByPath(path)
      }
      it
    }
  }

  fun addVirtualFileListener() {
  }

  fun removeVirtualFileListener() {
  }

  /**
   * Delete the selected virtual file and it's children if it is a directory
   * @param requestor requester class for further processing
   * @param vFile virtual file instance to delete
   */
  @Throws(IOException::class)
  fun deleteFile(requestor: Any?, vFile: MFVirtualFile) {
    val event = listOf(VFileDeleteEvent(requestor, vFile, false))
    vFile.validWriteLock {
      val parent = vFile.parent
      parent?.validWriteLock {
        if (vFile.children?.size != 0) {
          vFile.children?.forEach { it: MFVirtualFile -> deleteFile(requestor, it) }
        }
        sendMFVfsChangesTopic().before(event)
        if (fsGraph.removeEdge(parent, vFile) != null && fsGraph.removeVertex(vFile)) {
          val storageId = fileIdToStorageIdMap[vFile.id]
          if (storageId != null) {
            contentStorage.deleteRecord(storageId)
            fileIdToStorageIdMap.remove(vFile.id)
          }
          vFile.isValidInternal = false
          vFile.intermediateOldParent = parent
          val lastChar = parent.path.takeLast(1)
          vFile.intermediateOldPathInternal =
            if (lastChar == MFVirtualFileSystem.SEPARATOR) parent.path + vFile.name
            else parent.path + MFVirtualFileSystem.SEPARATOR + vFile.name
          initialContentConditions.remove(vFile)
          sendMFVfsChangesTopic().after(event)
          return@deleteFile
        }
      }
    }
    throw FsOperationException("delete", vFile)
  }

  @Throws(IOException::class)
  fun moveFile(requestor: Any?, vFile: MFVirtualFile, newParent: MFVirtualFile) {
    moveOrCopyFileInternal(requestor, vFile, newParent, copyName = null, replace = false)
  }

  @Throws(IOException::class)
  fun moveFileAndReplace(requestor: Any?, vFile: MFVirtualFile, newParent: MFVirtualFile) {
    moveOrCopyFileInternal(requestor, vFile, newParent, copyName = null, replace = true)
  }

  /**
   * Move or copy file internal function
   * @param requestor requester class for further processing
   * @param vFile source virtual file to process move or copy
   * @param newParent target virtual file to process move or copy
   * @param copyName new virtual file name
   * @param replace flag to replace the target in case of "true" value
   */
  private fun moveOrCopyFileInternal(
    requestor: Any?,
    vFile: MFVirtualFile,
    newParent: MFVirtualFile,
    copyName: String?,
    replace: Boolean,
  ): MFVirtualFile {
    val event = listOf(
      if (copyName != null) {
        VFileCopyEvent(requestor, vFile, newParent, copyName)
      } else {
        VFileMoveEvent(requestor, vFile, newParent)
      }
    )
    sendMFVfsChangesTopic().before(event)
    var adding: MFVirtualFile? = null
    validWriteLock(vFile, newParent) {
      val oldParent = vFile.parent
      if (oldParent != null && newParent.isDirectory) {
        if (oldParent == newParent && copyName == null) {
          return vFile
        }
        val oldEdge = fsGraph.getEdge(oldParent, vFile)
        if (oldEdge != null) {
          val alreadyExistingFile = newParent.children?.find {
            it.name == (copyName ?: vFile.name)
          }
          when {
            alreadyExistingFile != null && copyName != null && oldParent == newParent && replace -> {
              sendMFVfsChangesTopic().after(event)
              return vFile
            }

            alreadyExistingFile != null && replace -> { // move or copy in another folder with "replace" flag
              deleteFile(requestor, alreadyExistingFile)
            }

            alreadyExistingFile != null && !replace -> {
              throw FileAlreadyExistsException(alreadyExistingFile.path)
            }
          }
          val addingFile = if (copyName == null) {
            fsGraph.removeEdge(oldEdge)
            vFile
          } else {
            findOrCreate(this, newParent, copyName, vFile.attributes).apply {
              isValidInternal = true
            }
          }
          fsGraph.addEdge(newParent, addingFile, FSEdge(FSEdgeType.DIR))
          adding = addingFile
        }
      }
    }
    return adding?.also { sendMFVfsChangesTopic().after(event) }
      ?: throw FsOperationException(if (copyName != null) "copy" else "move", vFile)
  }

  /**
   * Rename the virtual file
   * @param requestor requester class for further processing
   * @param vFile virtual file to rename
   * @param newName new virtual file name
   */
  @Throws(IOException::class)
  fun renameFile(requestor: Any?, vFile: MFVirtualFile, newName: String) {
    val event = listOf(VFilePropertyChangeEvent(requestor, vFile, VirtualFile.PROP_NAME, vFile.name, newName, false))
    sendVfsChangesTopic().before(event)
    vFile.validReadLock {
      if (vFile.filenameInternal != newName) {
        vFile.filenameInternal = newName
      }
    }
    sendVfsChangesTopic().after(event)
  }

  @Throws(IOException::class)
  fun createChildDirectory(requestor: Any?, vDir: MFVirtualFile, dirName: String): MFVirtualFile {
    return createChild(requestor, vDir, dirName, true)
  }

  @Throws(IOException::class)
  fun createChildFile(requestor: Any?, vDir: MFVirtualFile, fileName: String): MFVirtualFile {
    return createChild(requestor, vDir, fileName, false)
  }

  /**
   * Create child virtual file with provided attributes
   * @param requestor class to describe the event requester
   * @param vDir virtual directory to create the virtual file in
   * @param name virtual file name
   * @param attributes attributes to create virtual file with
   */
  @Throws(IOException::class)
  fun createChildWithAttributes(
    requestor: Any?, vDir: MFVirtualFile, name: String, attributes: FileAttributes, sameNamesAllowed: Boolean
  ): MFVirtualFile {
    if (attributes.isSymLink) {
      throw IOException("Cannot create symlink without destination")
    }
    @Suppress("UnstableApiUsage")
    val event = listOf(VFileCreateEvent(requestor, vDir, name, attributes.isDirectory, attributes, null, false, null))
    sendMFVfsChangesTopic().before(event)
    val file = vDir.validWriteLock {
      if (!vDir.isDirectory) {
        throw NotDirectoryException(vDir.path)
      }
      if (list(vDir).any { it == name } && !sameNamesAllowed) {
        throw FileAlreadyExistsException(vDir.path + MFVirtualFileSystem.SEPARATOR + name)
      }
      MFVirtualFile(
        generateId(),
        name,
        attributes
      ).apply {
        if (fsGraph.addVertex(this) && fsGraph.addEdge(vDir, this, FSEdge(FSEdgeType.DIR))) {
          isValidInternal = true
        }
      }
    }
    runCatching {
      sendMFVfsChangesTopic().after(event)
    }
    return file
  }

  /**
   * Create child virtual file with default attributes
   * @param requestor requester class for further processing
   * @param vDir virtual directory to create the virtual file in
   * @param name virtual file name
   * @param isDir attribute to define is the virtual file directory
   */
  @Throws(IOException::class)
  private fun createChild(requestor: Any?, vDir: MFVirtualFile, name: String, isDir: Boolean): MFVirtualFile {
    return createChildWithAttributes(
      requestor, vDir, name, createAttributes(
        directory = isDir,
        special = false,
        symlink = false,
        hidden = false,
        length = 0,
        lastModified = System.nanoTime(),
        writable = true
      ),
      false
    )
  }

  /**
   * Check if file exists
   * @param file virtual file with attributes to check
   */
  fun exists(file: MFVirtualFile): Boolean {
    return file.read { fsGraph.containsVertex(file) }.also {
      if (!it) file.isValidInternal = false
    }
  }

  /**
   * List files in provided virtual directory
   * @param file virtual directory to list files
   */
  fun list(file: MFVirtualFile) = file.validReadLock({ arrayOf() }) {
    getChildren(file).run {
      map { it.name }.asArray()
    }
  }

  fun isDirectory(file: MFVirtualFile) = file.validReadLock(false) { file.isDirectory }

  fun getTimeStamp(file: MFVirtualFile): Long {
    return file.timeStamp
  }

  @Throws(IOException::class)
  fun setTimeStamp(file: MFVirtualFile, timeStamp: Long) {
    file.timeStamp = timeStamp
  }

  fun isWritable(file: MFVirtualFile) = file.validReadLock(false) { file.attributes.isWritable }

  fun isReadable(file: MFVirtualFile) = file.isReadable

  /**
   * Set virtual file writable
   * @param file virtual file to change
   * @param writableFlag writable flag to set to the file
   */
  @Throws(IOException::class)
  fun setWritable(file: MFVirtualFile, writableFlag: Boolean) {
    file.validWriteLock {
      if (file.isWritable != writableFlag) {
        val event = listOf(
          VFilePropertyChangeEvent(
            this,
            file,
            VirtualFile.PROP_WRITABLE,
            file.isWritableInternal,
            writableFlag,
            false
          )
        )
        sendMFVfsChangesTopic().before(event)
        file.isWritableInternal = writableFlag
        sendMFVfsChangesTopic().after(event)
      }
    }
  }

  fun setReadable(file: MFVirtualFile, readableFlag: Boolean) {
    file.isReadable = readableFlag
  }

  /**
   * Check is the file a symbolic link
   * @param file virtual file to check
   */
  fun isSymLink(file: MFVirtualFile): Boolean {
    return file.validReadLock(false) {
      fsGraph.outgoingEdgesOf(file).any { it.type.isSymlink() }
    }
  }

  fun resolveSymLink(file: MFVirtualFile): String? {
    return resolveAndGetSymlink(file)?.path
  }

  /**
   * Resolve and get symbolic link for the virtual file
   * @param file virtual file to check
   */
  fun resolveAndGetSymlink(file: MFVirtualFile): MFVirtualFile? {
    return file.validReadLock(null) {
      fsGraph.outgoingEdgesOf(file).firstOrNull { it.type.isSymlink() }?.let {
        fsGraph.getEdgeTarget(it)
      }
    }
  }

  @Throws(IOException::class)
  fun copyFile(
    requestor: Any?,
    virtualFile: MFVirtualFile,
    newParent: MFVirtualFile,
    copyName: String
  ): MFVirtualFile {
    return moveOrCopyFileInternal(requestor, virtualFile, newParent, copyName, false)
  }

  @Throws(IOException::class)
  fun copyFileAndReplace(
    requestor: Any?,
    virtualFile: MFVirtualFile,
    newParent: MFVirtualFile,
    copyName: String
  ): MFVirtualFile {
    return moveOrCopyFileInternal(requestor, virtualFile, newParent, copyName, true)
  }

  /**
   * Await for the initial file content if it is null at the beginning
   * @param file virtual file to get content for
   */
  private fun awaitForInitialContentIfNeeded(file: MFVirtualFile) {
    if (initialContentConditions[file] != null) {
      file.validWriteLock {
        initialContentConditions[file]?.await()
      }
    }
  }

  /**
   * Convert contents to byte array
   * @param file virtual file to convert
   */
  @Throws(IOException::class)
  fun contentsToByteArray(file: MFVirtualFile): ByteArray {
    awaitForInitialContentIfNeeded(file)
    return file.validReadLock {
      contentStorage.getBytes(getIdForStorageAccess(file))
    }
  }

  /**
   * Get input stream for the virtual file
   * @param file virtual file to get input stream
   */
  @Throws(IOException::class)
  fun getInputStream(file: MFVirtualFile): InputStream {
    awaitForInitialContentIfNeeded(file)
    return file.validReadLock {
      val storageStream = contentStorage.readStream(getIdForStorageAccess(file))
      object : DataInputStream(storageStream) {
        override fun close() {
          file.validReadLock {
            super.close()
          }
        }
      }
    }
  }

  /**
   * Get virtual file in storage ID for storage access. Works only for file and not for directory
   * @param file virtual file to get storage ID
   */
  private fun getIdForStorageAccess(file: MFVirtualFile): Int {
    val actualFile = if (isSymLink(file)) {
      resolveAndGetSymlink(file) ?: throw IOException("Symlink ${file.path} cannot be resolved to a file")
    } else file
    if (actualFile.isDirectory) {
      throw IOException("Cannot get storage data for directory")
    }
    return fileIdToStorageIdMap.getOrPut(actualFile.id) { contentStorage.createNewRecord() }
  }

  fun blockIOStreams(file: MFVirtualFile) {
    file.validWriteLock {
      initialContentConditions.getOrPut(file) { file.writeLock().newCondition() }
    }
  }

  fun unblockIOStreams(file: MFVirtualFile) {
    file.validWriteLock {
      initialContentConditions[file]?.signalAll()?.let {
        initialContentConditions.remove(file)
      }
    }
  }

  /**
   * Put initial content to the virtual file if it is possible
   * @param file virtual file to put content into
   * @param content content byte array to put in the virtual file
   * @return "true" if initial content is put, "false" otherwise
   */
  fun putInitialContentIfPossible(file: MFVirtualFile, content: ByteArray): Boolean {
    if (initialContentConditions[file] == null) {
      return false
    }
    return file.validWriteLock(false) {
      val condition = initialContentConditions[file] ?: return false
      try {
        contentStorage.writeBytes(getIdForStorageAccess(file), ByteArraySequence(content), false)
        initialContentConditions.remove(file)
        true
      } catch (e: IOException) {
        false
      } finally {
        condition.signalAll()
      }
    }
  }

  /**
   * Get file output stream
   * @param file virtual file to get output stream for
   * @param requestor requester class for further processing
   * @param modStamp file modification stamp
   * @param timeStamp file time stamp for the modification
   */
  @Throws(IOException::class)
  fun getOutputStream(file: MFVirtualFile, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream {
    return file.validWriteLock {
      val oldModStamp = file.modificationStamp

      val storageOutputStream = contentStorage.writeStream(getIdForStorageAccess(file))

      object : DataOutputStream(storageOutputStream) {
        private val isClosed = AtomicBoolean(false)
        override fun close() {
          if (isClosed.compareAndSet(false, true)) {
            val event = listOf(
              VFileContentChangeEvent(requestor, file, oldModStamp, timeStamp, false)
            )
            // When the output stream changes, the contents of the file change, so you need to send vfs changes topic
            try {
              sendVfsChangesTopic().before(event)
            } catch (ignored: InvalidPathException) {
              log.warn(ignored)
            }
            assertWriteAllowed()
            file.validWriteLock {
              super.close()
              file.timeStamp = timeStamp
              file.modificationStamp = modStamp
              initialContentConditions[file]?.signalAll()?.let {
                initialContentConditions.remove(file)
              }
            }
            try {
              sendVfsChangesTopic().after(event)
            } catch (ignored: InvalidPathException) {
              log.warn(ignored)
            }
          }
        }
      }
    }
  }

  /**
   * Get file content bytes length
   * @param file - virtual file to get bytes for
   */
  fun getLength(file: MFVirtualFile): Long {
    return file.validReadLock(0L) {
      try {
        val id = getIdForStorageAccess(file)
        contentStorage.getLength(id)
      } catch (e: IOException) {
        0L
      }
    }
  }

  fun isReadOnly() = false

  fun dispose() {
    isDisposed = true
    Disposer.dispose(contentStorage)
  }

  /**
   * Check is file valid
   * @param file virtual file to check
   */
  fun isFileValid(file: MFVirtualFile): Boolean {
    return if (isDisposed) {
      false
    } else {
      file.read {
        if (!file.isValidInternal) {
          if (fsGraph.containsVertex(file)) {
            addToCleanerQueue(file)
          }
          false
        } else {
          true
        }
      }
    }
  }

  private val cleanerLock = Any()

  /**
   * Add file to the cleaner queue
   * @param file virtual file to add
   */
  private fun addToCleanerQueue(file: MFVirtualFile) {
    synchronized(cleanerLock) {
      runBlocking {
        launch {
          file.write {
            fsGraph.removeVertex(file)
          }
        }
      }
    }
  }

  /**
   * Get virtual file parent
   * @param file virtual file to get parent
   */
  fun getParent(file: MFVirtualFile): MFVirtualFile? {
    return file.validReadLock(file::oldParentInternal) {
      (file != root).runIfTrue {
        fsGraph.predecessorsOf(file).singleOrNull {
          isEdgeOfType(it, file, FSEdgeType.DIR)
        }
      }
    }
  }

  fun getChildren(file: MFVirtualFile): Array<MFVirtualFile> {
    return file.validReadLock({ arrayOf() }) { getChildrenList(file).toTypedArray() }
  }

  /**
   * Get valid virtual file children list
   * @param file virtual file to get children for
   */
  fun getChildrenList(file: MFVirtualFile): List<MFVirtualFile> {
    return file.validReadLock({ listOf() }) {
      fsGraph.successorsOf(file).filter {
        isFileValid(it) && isEdgeOfType(file, it, FSEdgeType.DIR)
      }
    }
  }

  /**
   * Check is the edge of the provided type
   * @param source source virtual file to get the edge
   * @param target target virtual file to get the edge
   * @param type type of the edge to check
   */
  private fun isEdgeOfType(source: MFVirtualFile, target: MFVirtualFile, type: FSEdgeType): Boolean {
    return lock(source.readLock(), target.readLock()) {
      fsGraph.getEdge(source, target)?.type == type
    }
  }

  private fun String.formatPath(): String {
    val separator = MFVirtualFileSystem.SEPARATOR
    return replace(Regex("$separator+"), separator)
      .let {
        if (it.startsWith(separator)) {
          it.substringAfter(separator)
        } else {
          it
        }
      }
  }

  fun findFileByPathIfCached(path: String): MFVirtualFile? {
    return findFileByPath(path)
  }

  fun getRank() = 1

  fun getAttributes(file: MFVirtualFile) = file.write {
    file.isValidInternal.runIfTrue { file.attributes }
  }

  private fun <Edge> Graph<MFVirtualFile, Edge>.successorsOf(vertex: MFVirtualFile): List<MFVirtualFile> {
    return Graphs.successorListOf(this, vertex)
  }

  /**
   * Get predecessors of virtual file as vertex
   * @param vertex virtual file as vertex
   */
  private fun <Edge> Graph<MFVirtualFile, Edge>.predecessorsOf(vertex: MFVirtualFile): List<MFVirtualFile> {
    return if (vertex != root) {
      Graphs.predecessorListOf(this, vertex)
    } else {
      listOf()
    }
  }

}
