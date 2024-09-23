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

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.runIfTrue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/** Class that represents mainframe files in a virtual file system */
class MFVirtualFile internal constructor(
  private val fileId: Int,
  name: String,
  private val initialAttributes: FileAttributes,
) : VirtualFile(), VirtualFileWithId, ReadWriteLock by ReentrantReadWriteLock() {

  enum class PropName(propName: String) {
    IS_DIRECTORY("isDirectory")
  }

  companion object {
    private val fs by lazy { MFVirtualFileSystem.instance }
  }

  @Volatile
  internal var isValidInternal = false

  @Volatile
  internal var intermediateOldParent: MFVirtualFile? = null

  internal val oldParentInternal: MFVirtualFile
    get() = intermediateOldParent ?: throw IllegalStateException("Invalid file has no info about its old parent")

  @Volatile
  internal var intermediateOldPathInternal: String? = null

  private val oldPath: String
    get() = intermediateOldPathInternal
      ?: throw IllegalStateException("Invalid file has no info about its old path value")

  @Volatile
  internal var filenameInternal = name

  @Volatile
  internal var isWritableInternal = initialAttributes.isWritable

  @Volatile
  internal var isDirectoryInternal = initialAttributes.isDirectory

  val attributes
    get() = createAttributes(
      directory = isDirectoryInternal,
      special = initialAttributes.isSpecial,
      symlink = fs.isSymLink(this),
      hidden = initialAttributes.isHidden,
      length = length,
      lastModified = modificationStamp,
      writable = isWritableInternal
    )

  override fun getName(): String = filenameInternal

  override fun getNameSequence() = name

  override fun getPresentableName() = name

  override fun getFileSystem() = fs

  /** Get file path if the file is valid */
  @Suppress("RecursivePropertyAccessor")
  override fun getPath(): String {
    return if (isValid) {
      (parent?.path ?: "") + name + MFVirtualFileSystem.SEPARATOR
    } else {
      oldPath
    }
  }

  override fun isWritable() = fs.isWritable(this)

  override fun setWritable(writable: Boolean) {
    validWriteLock { isWritableInternal = writable }
  }

  @Volatile
  private var isReadableFlag = true

  var isReadable
    get() = validReadLock(false) { isReadableFlag }
    set(value) = validReadLock({}) { isReadableFlag = value }

  override fun isDirectory() = isDirectoryInternal

  /** Get virtual file if it is the symbolic link */
  override fun getCanonicalFile(): VirtualFile? {
    return if (fs.isSymLink(this)) {
      fs.model.resolveAndGetSymlink(this)
    } else null
  }

  override fun isValid() = fs.model.isFileValid(this)

  override fun getParent() = fs.model.getParent(this)

  /** Get children of the virtual directory */
  override fun getChildren() = isDirectoryInternal.runIfTrue { fs.model.getChildren(this) }

  /**
   * Find child in the virtual directory
   * @param name child name to search for
   */
  override fun findChild(name: String): MFVirtualFile? = isDirectory.runIfTrue {
    children?.firstOrNull { it.nameEquals(name) }
  }

  override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) =
    fs.getOutputStream(this, requestor, newModificationStamp, newTimeStamp)

  override fun contentsToByteArray(): ByteArray {
    return fs.contentsToByteArray(this)
  }

  @Volatile
  private var modStamp = 0L

  override fun getModificationStamp(): Long {
    return validReadLock(0L) { modStamp }
  }

  fun setModificationStamp(modStamp: Long) {
    validWriteLock { this.modStamp = modStamp; modCount.incrementAndGet() }
  }

  private val modCount = AtomicLong(0L)

  override fun getModificationCount(): Long {
    return modCount.get()
  }

  @Volatile
  private var timeStamp = 0L

  fun setTimeStamp(time: Long) = validWriteLock { timeStamp = time }

  override fun getTimeStamp() = validReadLock(0L) { timeStamp }

  override fun getLength() = fs.getLength(this)

  override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
    postRunnable?.run()
  }

  override fun getInputStream() = fs.getInputStream(this)

  override fun getId() = fileId

  /** Get file type */
  override fun getFileType(): FileType {
    val fileType = super.getFileType()
    return if (fileType is UnknownFileType) {
      PlainTextFileType.INSTANCE
    } else {
      fileType
    }
  }

  @Suppress("UNCHECKED_CAST")
  val cachedChildren
    get() = fs.model.getChildrenList(this)

  /**
   * Check if two files equal
   * @param other the second file to check
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MFVirtualFile

    if (fileId != other.fileId) return false

    return true
  }

  override fun hashCode(): Int {
    return fileId
  }

  val isRoot
    get() = id == MFVirtualFileSystem.ROOT_ID

}

@JvmOverloads
fun createAttributes(
  directory: Boolean,
  special: Boolean = false,
  symlink: Boolean = false,
  hidden: Boolean = false,
  length: Long = 0,
  lastModified: Long = System.nanoTime(),
  writable: Boolean = true
) = FileAttributes(directory, special, symlink, hidden, length, lastModified, writable)

inline fun <T> MFVirtualFile.validReadLock(default: T, block: () -> T): T {
  return this.genericLockOr(this.readLock(), { default }, block)
}

inline fun <T> MFVirtualFile.validReadLock(default: () -> T, block: () -> T): T {
  return this.genericLockOr(this.readLock(), default, block)
}

inline fun <T> MFVirtualFile.validReadLock(
  exception: Exception = InvalidFileException(this), block: () -> T
): T {
  return this.genericLockOr(this.readLock(), { throw exception }, block)
}

inline fun <T> MFVirtualFile.validWriteLock(default: T, block: () -> T): T {
  return this.genericLockOr(this.writeLock(), { default }, block)
}

inline fun <T> MFVirtualFile.validWriteLock(default: () -> T, block: () -> T): T {
  return this.genericLockOr(this.writeLock(), default, block)
}

@JvmName("validWriteLockOrThrow")
inline fun <T> MFVirtualFile.validWriteLock(
  exception: () -> Throwable = { InvalidFileException(this) }, block: () -> T
): T {
  return this.genericLockOr(this.writeLock(), { throw exception() }, block)
}

inline fun <T> validReadLock(vararg files: MFVirtualFile, default: T, block: () -> T): T {
  return genericVarargLockOr(files, files.map { it.readLock() }.toTypedArray(), { { default } }, block)
}

inline fun <T> validReadLock(
  vararg files: MFVirtualFile,
  exception: (MFVirtualFile) -> Exception = { InvalidFileException(it) },
  block: () -> T
): T {
  return genericVarargLockOr(
    files,
    files.map { it.readLock() }.toTypedArray(),
    { file ->
      if (file != null) {
        throw exception(file)
      } else {
        throw IllegalArgumentException("No virtual files provided to lock")
      }
    },
    block
  )
}

inline fun <reified T> validWriteLock(vararg files: MFVirtualFile, default: T, block: () -> T): T {
  return genericVarargLockOr(files, files.map { it.writeLock() }.toTypedArray(), { { default } }, block)
}

inline fun <T> validWriteLock(
  vararg files: MFVirtualFile,
  exception: (MFVirtualFile) -> Exception = { InvalidFileException(it) },
  block: () -> T
): T {
  return genericVarargLockOr(
    files,
    files.map { it.writeLock() }.toTypedArray(),
    { file ->
      if (file != null) {
        throw exception(file)
      } else {
        throw IllegalArgumentException("No virtual files provided to lock")
      }
    },
    block
  )
}

/**
 * Run the block of code with the specified generic lock for the file or for the list of files
 * @param files the files to get the lock for
 * @param locks the locks for the files
 * @param default the default action to be handled if the file is not valid
 * @param block the block of code to be handled with the lock
 */
@PublishedApi
internal inline fun <T> genericVarargLockOr(
  files: Array<out MFVirtualFile>, locks: Array<Lock>, default: (MFVirtualFile?) -> () -> T, block: () -> T
): T {
  return if (files.size == locks.size) {
    when (files.size) {
      0 -> default(null)()
      1 -> {
        files[0].genericLockOr(locks[0], default(files[0]), block)
      }

      else -> {
        files.find { !it.isValid }?.let { return default(it)() }
        lock(*locks) {
          files.find { !it.isValid }?.let { return default(it)() }
          block()
        }
      }
    }
  } else {
    default(null)()
  }
}

/**
 * Run the block of code with the specified generic lock for the file
 * @param lock the lock for the file
 * @param default the default action to be handled if the file is not valid
 * @param block the block of code to be handled with the lock
 */
@PublishedApi
internal inline fun <T> MFVirtualFile.genericLockOr(
  lock: Lock, default: () -> T, block: () -> T
): T {
  return if (isValid) {
    lock.withLock {
      if (isValid) {
        block()
      } else {
        default()
      }
    }
  } else {
    default()
  }
}
