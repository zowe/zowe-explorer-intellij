package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import eu.ibagroup.formainframe.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jgrapht.Graph
import org.jgrapht.Graphs
import org.jgrapht.graph.DirectedMultigraph
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NotDirectoryException
import java.util.concurrent.atomic.AtomicInteger

class FsOperationException(operationName: String, file: MFVirtualFile) : IOException(
  "Cannot perform $operationName on ${file.path}"
)

internal fun sendVfsChangesTopic() = sendTopic(VirtualFileManager.VFS_CHANGES)

enum class FSEdgeType {
  DIR, SOFT_LINK, HARD_LINK
}

class FSEdge @JvmOverloads constructor(val type: FSEdgeType = FSEdgeType.DIR)

class MFVirtualFileSystemModel : VirtualFileSystemModel<MFVirtualFile> {

  companion object {
    private val counter = AtomicInteger(MFVirtualFileSystem.ROOT_ID)
  }

  private val fsGraph = AsConcurrentGraph(
    DirectedMultigraph<MFVirtualFile, FSEdge>(FSEdge::class.java)
  )

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
    //assertReadAllowed()
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
    //assertReadAllowed()
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
    throw UnsupportedOperationException()
  }

  override fun removeVirtualFileListener(listener: VirtualFileListener) {
    throw UnsupportedOperationException()
  }

  @Throws(IOException::class)
  override fun deleteFile(requestor: Any?, vFile: MFVirtualFile) {
    //assertWriteAllowed()
    val event = listOf(VFileDeleteEvent(requestor, vFile, false))
    invokeLater { sendVfsChangesTopic().before(event) }
    var successful = false
    vFile.validWriteLock {
      val parent = vFile.parent
      parent?.validWriteLock {
        if (vFile.children?.size != 0) {
          vFile.children?.forEach { deleteFile(requestor, it) }
        }
        if (fsGraph.removeEdge(parent, vFile) != null && fsGraph.removeVertex(vFile)) {
          successful = true
        }
      }
    }
    if (successful) {
      invokeLater { sendVfsChangesTopic().after(event) }
      return
    } else {
      throw FsOperationException("delete", vFile)
    }
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
    //assertWriteAllowed()
    val event = listOf(
      if (copyName != null) {
        VFileCopyEvent(requestor, vFile, newParent, copyName)
      } else {
        VFileMoveEvent(requestor, vFile, newParent)
      }
    )
    invokeLater { sendVfsChangesTopic().after(event) }
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
            MFVirtualFile(generateId(), copyName, vFile.attributes)
          }
          fsGraph.addEdge(newParent, addingFile, FSEdge(FSEdgeType.DIR))
          adding = addingFile
        }
      }
    }
    return adding?.also { invokeLater { sendVfsChangesTopic().before(event) } }
      ?: throw FsOperationException(if (copyName != null) "copy" else "move", vFile)
  }

  @Throws(IOException::class)
  override fun renameFile(requestor: Any?, vFile: MFVirtualFile, newName: String) {
    val event = listOf(VFilePropertyChangeEvent(requestor, vFile, VirtualFile.PROP_NAME, vFile.name, newName, false))
    invokeLater { sendVfsChangesTopic().before(event) }
    vFile.validReadLock {
      if (vFile.filenameInternal != newName) {
        vFile.filenameInternal = newName
      }
    }
    invokeLater { sendVfsChangesTopic().after(event) }
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
    invokeLater { sendVfsChangesTopic().before(event) }
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
    invokeLater { sendVfsChangesTopic().after(event) }
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
    //assertReadAllowed()
    return lock(file.readLock()) { fsGraph.containsVertex(file) }.also {
      if (!it) file.isValidInternal = false
    }
  }

  override fun list(file: MFVirtualFile) = file.validReadLock {
    getChildren(file)?.run {
      map { it.name }.asArray()
    } ?: arrayOf()
  }

  override fun isDirectory(file: MFVirtualFile) = file.validReadLock(false) { file.isDirectory }

  override fun getTimeStamp(file: MFVirtualFile): Long {
    TODO("Not yet implemented")
  }

  @Throws(IOException::class)
  override fun setTimeStamp(file: MFVirtualFile, timeStamp: Long) {
    TODO("Not yet implemented")
  }

  override fun isWritable(file: MFVirtualFile) = file.validReadLock { file.attributes.isWritable }

  @Throws(IOException::class)
  override fun setWritable(file: MFVirtualFile, writableFlag: Boolean) {
    file.validWriteLock {
      file.isWritableInternal = writableFlag
    }
  }

  override fun isSymLink(file: MFVirtualFile): Boolean {
    return fsGraph.outgoingEdgesOf(file).any { it.type == FSEdgeType.SOFT_LINK || it.type == FSEdgeType.HARD_LINK }
  }

  override fun resolveSymLink(file: MFVirtualFile): String? {
    return resolveAndGetSymlink(file)?.path
  }

  fun resolveAndGetSymlink(file: MFVirtualFile): MFVirtualFile? = null

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

  @Throws(IOException::class)
  override fun contentsToByteArray(file: MFVirtualFile): ByteArray {
    TODO("Not yet implemented")
  }

  @Throws(IOException::class)
  override fun getInputStream(file: MFVirtualFile): InputStream {
    TODO("Not yet implemented")
  }

  @Throws(IOException::class)
  override fun getOutputStream(file: MFVirtualFile, requestor: Any?, modStamp: Long, timeStamp: Long): OutputStream {
    TODO("Not yet implemented")
  }

  override fun getLength(file: MFVirtualFile): Long {
    return 0
  }

  override fun isReadOnly() = false

  fun isFileValid(file: MFVirtualFile): Boolean {
    return lock(file.readLock()) {
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

  private val cleanerLock = Any()

  private fun addToCleanerQueue(file: MFVirtualFile) {
    synchronized(cleanerLock) {
      GlobalScope.launch {
        lock(file.writeLock()) {
          fsGraph.removeVertex(file)
        }
      }
    }
  }

  fun getParent(file: MFVirtualFile): MFVirtualFile? {
    return file.validReadLock(null) {
      (file != root).runIfTrue {
        fsGraph.predecessorsOf(file).singleOrNull {
          isEdgeOfType(it, file, FSEdgeType.DIR)
        }
      }
    }
  }

  fun getChildren(file: MFVirtualFile): Array<MFVirtualFile> {
    return file.validReadLock { getChildrenList(file).toTypedArray() }
  }

  fun getChildrenList(file: MFVirtualFile): List<MFVirtualFile> {
    return file.validReadLock(IOException("")) {
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
    return replace(Regex("\\$separator+"), separator).let {
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

  override fun getAttributes(file: MFVirtualFile) = lock(file.writeLock()) {
    file.isValidInternal.runIfTrue { file.attributes }
  }

}

fun <Vertex, Edge> Graph<Vertex, Edge>.successorsOf(vertex: Vertex): List<Vertex> {
  return Graphs.successorListOf(this, vertex)
}

fun <Vertex, Edge> Graph<Vertex, Edge>.predecessorsOf(vertex: Vertex): List<Vertex> {
  return Graphs.predecessorListOf(this, vertex)
}