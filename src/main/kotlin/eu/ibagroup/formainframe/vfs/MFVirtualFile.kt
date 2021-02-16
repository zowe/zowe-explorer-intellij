package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.runIfTrue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

val CHILDREN_LOADED = KeyWithDefaultValue.create("childrenLoaded", false)
//val IS_VALID = KeyWithDefaultValue.create("isValid", false)

class MFVirtualFile internal constructor(
  private val fileId: Int,
  name: String,
  private val initialAttributes: FileAttributes,
  //childrenLoaded: Boolean = true,
) : NewVirtualFile(), ReadWriteLock by ReentrantReadWriteLock() {

  companion object {
    private val fs = MFVirtualFileSystem.instance
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

//  @Volatile
//  internal var childrenLoadedInternal = childrenLoaded
//
//  fun setChildrenLoaded() {
//    validWriteLock { childrenLoadedInternal = true }
//  }

  @Volatile
  internal var isWritableInternal = initialAttributes.isWritable

  val attributes
    get() = createAttributes(
      directory = isDirectory,
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
    TODO("Not yet implemented")
  }

  override fun isDirectory() = initialAttributes.isDirectory

  override fun getCanonicalFile(): NewVirtualFile? {
    TODO("Not yet implemented")
  }

  override fun isValid() = fs.model.isFileValid(this)

  override fun getParent() = fs.model.getParent(this)

  override fun getChildren() = isDirectory.runIfTrue { fs.model.getChildren(this) }

  override fun findChild(name: String): MFVirtualFile? = isDirectory.runIfTrue {
    children?.firstOrNull { it.nameEquals(name) }
  }

//  var childrenLoaded
//    get() = getUserData(CHILDREN_LOADED)!!
//    set(value) = putUserData(CHILDREN_LOADED, value)

  override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) =
    fs.getOutputStream(this, requestor, newModificationStamp, newTimeStamp)

  override fun contentsToByteArray(): ByteArray {
    return fs.contentsToByteArray(this)
  }

  override fun setTimeStamp(time: Long) {
    TODO("Not yet implemented")
  }

  override fun getTimeStamp() = fs.getTimeStamp(this)

  override fun getLength() = fs.getLength(this)

  override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
    postRunnable?.run()
  }

  override fun getInputStream() = fs.getInputStream(this)

  override fun getId() = fileId

  override fun refreshAndFindChild(name: String): NewVirtualFile? {
    TODO("Not yet implemented")
  }

  override fun findChildIfCached(name: String) = findChild(name)

  override fun getUrl(): String {
    return super.getUrl()
  }

  override fun markDirty() {
    TODO("Not yet implemented")
  }

  override fun markDirtyRecursively() {
    TODO("Not yet implemented")
  }

  override fun isDirty(): Boolean {
    TODO("Not yet implemented")
  }

  override fun markClean() {
    TODO("Not yet implemented")
  }

  override fun getExtension(): String {
    return name.split(".").last()
  }

  override fun getNameWithoutExtension(): String {
    return name.split(".").dropLast(1).joinToString(separator = ".")
  }

  @Suppress("UNCHECKED_CAST")
  override fun getCachedChildren() = fs.model.getChildrenList(this)

  override fun iterInDbChildren() = cachedChildren

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
  exception: Exception = InvalidVirtualFileAccessException(this), block: () -> T
): T {
  return this.genericLockOr(this.readLock(), { throw exception }, block)
}

inline fun <T> MFVirtualFile.validWriteLock(default: T, block: () -> T): T {
  return this.genericLockOr(this.writeLock(), { default }, block)
}

inline fun <T> MFVirtualFile.validWriteLock(default: () -> T, block: () -> T): T {
  return this.genericLockOr(this.writeLock(), default, block)
}

inline fun <T> MFVirtualFile.validWriteLock(
  exception: Exception = InvalidVirtualFileAccessException(this), block: () -> T
): T {
  return this.genericLockOr(this.writeLock(), { throw exception }, block)
}

inline fun <T> validReadLock(vararg files: MFVirtualFile, default: T, block: () -> T): T {
  return genericVarargLockOr(files, files.map { it.readLock() }.toTypedArray(), { { default } }, block)
}

inline fun <T> validReadLock(
  vararg files: MFVirtualFile,
  exception: (MFVirtualFile) -> Exception = { InvalidVirtualFileAccessException(it) },
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
  exception: (MFVirtualFile) -> Exception = { InvalidVirtualFileAccessException(it) },
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

@PublishedApi
internal inline fun <T> MFVirtualFile.genericLockOr(
  lock: Lock, default: () -> T, block: () -> T
): T {
  return if (isValid) {
    lock(lock) {
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