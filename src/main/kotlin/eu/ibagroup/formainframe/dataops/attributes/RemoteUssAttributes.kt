package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.utils.Copyable
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.r2z.FileMode
import eu.ibagroup.r2z.FileModeValue
import eu.ibagroup.r2z.UssFile
import eu.ibagroup.r2z.XIBMDataType

private const val CURRENT_DIR_NAME = "."

private fun constructPath(rootPath: String, ussFile: UssFile): String {
  return when {
    ussFile.name.isEmpty() || ussFile.name == CURRENT_DIR_NAME -> {
      rootPath
    }
    rootPath == USS_DELIMITER -> {
      rootPath + ussFile.name
    }
    else -> {
      rootPath + USS_DELIMITER + ussFile.name
    }
  }
}

const val USS_DELIMITER = "/"

data class RemoteUssAttributes(
  val path: String,
  val isDirectory: Boolean,
  val fileMode: FileMode?,
  override val url: String,
  override val requesters: MutableList<UssRequester>,
  override val length: Long = 0L,
  val uid: Long? = null,
  val owner: String? = null,
  val gid: Long? = null,
  val groupId: String? = null,
  val modificationTime: String? = null,
  val symlinkTarget: String? = null
) : MFRemoteFileAttributes<UssRequester>, Copyable {

  constructor(rootPath: String, ussFile: UssFile, url: String, connectionConfig: ConnectionConfig) : this(
    path = constructPath(rootPath, ussFile),
    isDirectory = ussFile.isDirectory,
    fileMode = ussFile.fileMode,
    url = url,
    requesters = mutableListOf(UssRequester(connectionConfig)),
    length = ussFile.size ?: 0L,
    uid = ussFile.uid,
    owner = ussFile.user,
    gid = ussFile.gid,
    groupId = ussFile.groupId,
    modificationTime = ussFile.modificationTime,
    symlinkTarget = ussFile.target
  )

  override fun clone(): FileAttributes {
    return this.clone(RemoteUssAttributes::class.java)
  }

  val isSymlink
    get() = symlinkTarget != null

  override val name
    get() = path.split(USS_DELIMITER).last()

  val parentDirPath
    get() = path.substring(1).split(USS_DELIMITER).dropLast(1).joinToString(separator = USS_DELIMITER)

  val isWritable: Boolean
    get() {
      val hasFileOwnerInRequesters = requesters.any { username(it.connectionConfig).equals(owner, ignoreCase = true) }
      val mode = if (hasFileOwnerInRequesters) {
        fileMode?.owner
      } else {
        fileMode?.all
      }
      return mode == FileModeValue.WRITE.mode
        || mode == FileModeValue.WRITE_EXECUTE.mode
        || mode == FileModeValue.READ_WRITE.mode
        || mode == FileModeValue.READ_WRITE_EXECUTE.mode
    }

  val isReadable: Boolean
    get() {
      val hasFileOwnerInRequesters = requesters.any { username(it.connectionConfig).equals(owner, ignoreCase = true) }
      val mode = if (hasFileOwnerInRequesters) {
        fileMode?.owner
      } else {
        fileMode?.all
      }
      return mode == FileModeValue.READ.mode
        || mode == FileModeValue.READ_WRITE.mode
        || mode == FileModeValue.READ_EXECUTE.mode
        || mode == FileModeValue.READ_WRITE_EXECUTE.mode
    }

  val isExecutable: Boolean
    get() {
      val hasFileOwnerInRequesters = requesters.any { username(it.connectionConfig).equals(owner, ignoreCase = true) }
      val mode = if (hasFileOwnerInRequesters) {
        fileMode?.owner
      } else {
        fileMode?.all
      }
      return mode == FileModeValue.EXECUTE.mode
        || mode == FileModeValue.READ_EXECUTE.mode
        || mode == FileModeValue.WRITE_EXECUTE.mode
        || mode == FileModeValue.READ_WRITE_EXECUTE.mode
    }

  override var contentMode: XIBMDataType= XIBMDataType(XIBMDataType.Type.TEXT)

  override val isCopyPossible: Boolean
    get() = true

  override val isPastePossible: Boolean
    get() = isDirectory && isWritable

}