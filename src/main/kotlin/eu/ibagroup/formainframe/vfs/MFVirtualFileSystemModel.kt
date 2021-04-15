package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.ByteArraySequence
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jgrapht.Graph
import org.jgrapht.Graphs
import org.jgrapht.graph.DirectedMultigraph
import java.io.*
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NotDirectoryException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition

class FsOperationException(operationName: String, file: MFVirtualFile) : IOException(
  "Cannot perform $operationName on ${file.path}"
)

internal fun sendVfsChangesTopic() = sendTopic(VirtualFileManager.VFS_CHANGES)

enum class FSEdgeType {
  DIR, SOFT_LINK, HARD_LINK;

  fun isSymlink(): Boolean {
    return this == SOFT_LINK || this == HARD_LINK
  }
}

private const val VFS_CONTENT_STORAGE_NAME = "mf_vfs_contents"

class FSEdge @JvmOverloads constructor(val type: FSEdgeType = FSEdgeType.DIR)

class MFVirtualFileSystemModel : VirtualFileSystemModel<MFVirtualFile> {

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
    generateId(), MFVirtualFileSystem.ROOT_NAME, createAttributes(
      directory = true,
      special = false,
      symlink = false,
      hidden = false,
      length = 0,
      lastModified = System.nanoTime(),
      writable = false
    )
  ).apply {
    if (fsGraph.addVertex(this)) {
      isValidInternal = true
    }
  }

  private fun generateId() = counter.getAndIncrement()

  override fun getProtocol() = MFVirtualFileSystem.PROTOCOL

  @Throws(IOException::class)
  fun findOrCreate(
    requestor: Any?, vDir: MFVirtualFile, name: String, attributes: FileAttributes
  ): MFVirtualFile {
    var found = vDir.findChild(name)
    if (found == null) {
      found = createChildWithAttributes(requestor, vDir, name, attributes)
    }
    return found
  }

  fun findFileByPath(pathElements: List<String>): MFVirtualFile? {
    var pointerIndex = 1
    return FilteringBFSIterator(fsGraph, root) { v, e ->
      v.validReadLock(false) {
        (pointerIndex < pathElements.size
            && e.type == FSEdgeType.DIR
            && pathElements[pointerIndex] == v.name).also { successful ->
          if (successful) ++pointerIndex
        }
      }
    }.stream().skip(pathElements.size - 1L).findAny().nullable
  }

  override fun findFileByPath(path: String): MFVirtualFile? {
    val pathElements = path.formatPath().split(MFVirtualFileSystem.SEPARATOR)
    return findFileByPath(pathElements)
  }

  override fun refresh(asynchronous: Boolean) {
  }

  override fun refreshAndFindFileByPath(path: String): MFVirtualFile? {
    return findFileByPath(path).let {
      if (it == null) {
        refresh(false)
        return findFileByPath(path)
      }
      it
    }
  }

  override fun addVirtualFileListener(listener: VirtualFileListener) {

  }

  override fun removeVirtualFileListener(listener: VirtualFileListener) {
  }

  @Throws(IOException::class)
  override fun deleteFile(requestor: Any?, vFile: MFVirtualFile) {
    val event = listOf(VFileDeleteEvent(requestor, vFile, false))
    vFile.validWriteLock {
      val parent = vFile.parent
      parent?.validWriteLock {
        if (vFile.children?.size != 0) {
          vFile.children?.forEach { deleteFile(requestor, it) }
        }
        sendVfsChangesTopic().before(event)
        if (fsGraph.removeEdge(parent, vFile) != null && fsGraph.removeVertex(vFile)) {
          val storageId = fileIdToStorageIdMap[vFile.id]
          if (storageId != null) {
            contentStorage.deleteRecord(storageId)
            fileIdToStorageIdMap.remove(vFile.id)
          }
          vFile.isValidInternal = false
          vFile.intermediateOldParent = parent
          vFile.intermediateOldPathInternal = parent.path + MFVirtualFileSystem.SEPARATOR + vFile.name
          initialContentConditions.remove(vFile)
          sendVfsChangesTopic().after(event)
          return
        }
      }
    }
    throw FsOperationException("delete", vFile)
  }

  @Throws(IOException::class)
  override fun moveFile(requestor: Any?, vFile: MFVirtualFile, newParent: MFVirtualFile) {
    moveOrCopyFileInternal(requestor, vFile, newParent, copyName = null, replace = false)
  }

  @Throws(IOException::class)
  fun moveFileAndReplace(requestor: Any?, vFile: MFVirtualFile, newParent: MFVirtualFile) {
    moveOrCopyFileInternal(requestor, vFile, newParent, copyName = null, replace = true)
  }

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
    sendVfsChangesTopic().after(event)
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
            it.name == copyName ?: vFile.name
          }
          when {
            alreadyExistingFile != null && copyName != null && oldParent == newParent && replace -> {
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
            MFVirtualFile(generateId(), copyName, vFile.attributes).apply {
              isValidInternal = true
            }
          }
          fsGraph.addEdge(newParent, addingFile, FSEdge(FSEdgeType.DIR))
          adding = addingFile
        }
      }
    }
    return adding?.also { sendVfsChangesTopic().before(event) }
      ?: throw FsOperationException(if (copyName != null) "copy" else "move", vFile)
  }

  @Throws(IOException::class)
  override fun renameFile(requestor: Any?, vFile: MFVirtualFile, newName: String) {
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
  override fun createChildDirectory(requestor: Any?, vDir: MFVirtualFile, dirName: String): MFVirtualFile {
    return createChild(requestor, vDir, dirName, true)
  }

  @Throws(IOException::class)
  override fun createChildFile(requestor: Any?, vDir: MFVirtualFile, fileName: String): MFVirtualFile {
    return createChild(requestor, vDir, fileName, false)
  }

  @Throws(IOException::class)
  fun createChildWithAttributes(
    requestor: Any?, vDir: MFVirtualFile, name: String, attributes: FileAttributes
  ): MFVirtualFile {
    if (attributes.isSymLink) {
      throw IOException("Cannot create symlink without destination")
    }
    @Suppress("UnstableApiUsage")
    val event = listOf(VFileCreateEvent(requestor, vDir, name, attributes.isDirectory, attributes, null, false, null))
    sendVfsChangesTopic().before(event)
    val file = vDir.validWriteLock {
      if (!vDir.isDirectory) {
        throw NotDirectoryException(vDir.path)
      }
      if (list(vDir).any { it == name }) {
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
    sendVfsChangesTopic().after(event)
    return file
  }


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
      )
    )
  }

  override fun exists(file: MFVirtualFile): Boolean {
    return file.read { fsGraph.containsVertex(file) }.also {
      if (!it) file.isValidInternal = false
    }
  }

  override fun list(file: MFVirtualFile) = file.validReadLock({ arrayOf() }) {
    getChildren(file).run {
      map { it.name }.asArray()
    }
  }

  override fun isDirectory(file: MFVirtualFile) = file.validReadLock(false) { file.isDirectory }

  override fun getTimeStamp(file: MFVirtualFile): Long {
    return file.timeStamp
  }

  @Throws(IOException::class)
  override fun setTimeStamp(file: MFVirtualFile, timeStamp: Long) {
    file.timeStamp = timeStamp
  }

  override fun isWritable(file: MFVirtualFile) = file.validReadLock(false) { file.attributes.isWritable }

  fun isReadable(file: MFVirtualFile) = file.isReadable

  @Throws(IOException::class)
  override fun setWritable(file: MFVirtualFile, writableFlag: Boolean) {
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
        sendVfsChangesTopic().before(event)
        file.isWritableInternal = writableFlag
        sendVfsChangesTopic().after(event)
      }
    }
  }

  fun setReadable(file: MFVirtualFile, readableFlag: Boolean) {
    file.isReadable = readableFlag
  }

  override fun isSymLink(file: MFVirtualFile): Boolean {
    return file.validReadLock(false) {
      fsGraph.outgoingEdgesOf(file).any { it.type.isSymlink() }
    }
  }

  override fun resolveSymLink(file: MFVirtualFile): String? {
    return resolveAndGetSymlink(file)?.path
  }

  fun resolveAndGetSymlink(file: MFVirtualFile): MFVirtualFile? {
    return file.validReadLock(null) {
      fsGraph.outgoingEdgesOf(file).firstOrNull { it.type.isSymlink() }?.let {
        fsGraph.getEdgeTarget(it)
      }
    }
  }

  @Throws(IOException::class)
  override fun copyFile(
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

  private fun awaitForInitialContentIfNeeded(file: MFVirtualFile) {
    if (initialContentConditions[file] != null) {
      file.validWriteLock {
        initialContentConditions[file]?.await()
      }
    }
  }

  @Throws(IOException::class)
  override fun contentsToByteArray(file: MFVirtualFile): ByteArray {
    awaitForInitialContentIfNeeded(file)
    return file.validReadLock {
      contentStorage.getBytes(getIdForStorageAccess(file))
    }
  }

  @Throws(IOException::class)
  override fun getInputStream(file: MFVirtualFile): InputStream {
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

  @Throws(IOException::class)
  override fun getOutputStream(file: MFVirtualFile, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream {
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
            sendVfsChangesTopic().before(event)
            assertWriteAllowed()
            file.validWriteLock {
              super.close()
              file.timeStamp = timeStamp
              file.modificationStamp = modStamp
              initialContentConditions[file]?.signalAll()?.let {
                initialContentConditions.remove(file)
              }
            }
            sendVfsChangesTopic().after(event)
          }
        }
      }
    }
  }

  override fun getLength(file: MFVirtualFile): Long {
    return file.validReadLock(0L) {
      try {
        val id = getIdForStorageAccess(file)
        contentStorage.getLength(id)
      } catch (e: IOException) {
        0L
      }
    }
  }

  override fun isReadOnly() = false

  override fun dispose() {
    isDisposed = true
    Disposer.dispose(contentStorage)
  }

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

  fun getChildrenList(file: MFVirtualFile): List<MFVirtualFile> {
    return file.validReadLock({ listOf() }) {
      fsGraph.successorsOf(file).filter {
        isFileValid(it) && isEdgeOfType(file, it, FSEdgeType.DIR)
      }
    }
  }

  private fun isEdgeOfType(source: MFVirtualFile, target: MFVirtualFile, type: FSEdgeType): Boolean {
    return lock(source.readLock(), target.readLock()) {
      fsGraph.getEdge(source, target)?.type == type
    }
  }

  private fun String.formatPath(): String {
    val separator = MFVirtualFileSystem.SEPARATOR
    return replace(Regex("$separator+"), separator).let {
      if (it.startsWith(separator)) {
        it.substringAfter(separator)
      } else {
        it
      }
    }
  }

  override fun extractRootPath(normalizedPath: String) = with(MFVirtualFileSystem) {
    ROOT_NAME + SEPARATOR
  }

  override fun findFileByPathIfCached(path: String): MFVirtualFile? {
    return findFileByPath(path)
  }

  override fun getRank() = 1

  override fun getAttributes(file: MFVirtualFile) = file.write {
    file.isValidInternal.runIfTrue { file.attributes }
  }

  private fun <Edge> Graph<MFVirtualFile, Edge>.successorsOf(vertex: MFVirtualFile): List<MFVirtualFile> {
    return Graphs.successorListOf(this, vertex)
  }

  private fun <Edge> Graph<MFVirtualFile, Edge>.predecessorsOf(vertex: MFVirtualFile): List<MFVirtualFile> {
    return if (vertex != root) {
      Graphs.predecessorListOf(this, vertex)
    } else {
      listOf()
    }
  }

}