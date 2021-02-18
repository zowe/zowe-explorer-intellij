package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.r2z.FileMode
import eu.ibagroup.r2z.FileModeValue
import eu.ibagroup.r2z.UssFile

private fun constructPath(rootPath: String, ussFile: UssFile): String {
  return when {
    ussFile.name.isEmpty() -> {
      rootPath
    }
    rootPath == "/" -> {
      rootPath + ussFile.name
    }
    else -> {
      rootPath + "/" + ussFile.name
    }
  }
}

data class RemoteUssAttributes(
  val path: String,
  val isDirectory: Boolean,
  val fileMode: FileMode?,
  override val url: String,
  override val requesters: MutableList<UssRequester>,
  val size: Int? = null,
  val uid: Int? = null,
  val owner: String? = null,
  val gid: Int? = null,
  val groupId: String? = null,
  val modificationTime: String? = null,
  val symlinkTarget: String? = null
) : MFRemoteFileAttributes<UssRequester> {

  constructor(rootPath: String, ussFile: UssFile, url: String, connectionConfig: ConnectionConfig) : this(
    path = constructPath(rootPath, ussFile),
    isDirectory = ussFile.isDirectory,
    fileMode = ussFile.fileMode,
    url = url,
    requesters = mutableListOf(UssRequester(connectionConfig)),
    size = ussFile.size,
    uid = ussFile.uid,
    owner = ussFile.user,
    gid = ussFile.gid,
    groupId = ussFile.groupId,
    modificationTime = ussFile.modificationTime,
    symlinkTarget = ussFile.target
  )

  override fun clone(): VFileInfoAttributes {
    return this.clone(RemoteUssAttributes::class.java)
  }

  val isSymlink
    get() = symlinkTarget != null

  val name
    get() = path.split("/").last()

  val parentDirPath
    get() = path.substring(1).split("/").dropLast(1).joinToString(separator = "/")

  val isWritable: Boolean
    get() {
      val hasFileOwnerInRequesters = requesters.any { username(it.connectionConfig) == owner }
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

}