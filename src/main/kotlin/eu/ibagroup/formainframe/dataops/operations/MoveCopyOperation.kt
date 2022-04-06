package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.explorer.Explorer


class MoveCopyOperation(
  val source: VirtualFile,
  val sourceAttributes: FileAttributes?,
  val destination: VirtualFile,
  val destinationAttributes: FileAttributes?,
  val isMove: Boolean,
  val forceOverwriting: Boolean,
  val newName: String?,
  val explorer: Explorer<*>? = null
) : UnitOperation {
  constructor(
    source: VirtualFile,
    destination: VirtualFile,
    isMove: Boolean,
    forceOverwriting: Boolean,
    newName: String?,
    dataOpsManager: DataOpsManager,
    explorer: Explorer<*>? = null
  ) : this(
    source = source,
    sourceAttributes = dataOpsManager.tryToGetAttributes(source),
    destination = destination,
    destinationAttributes = dataOpsManager.tryToGetAttributes(destination),
    isMove = isMove,
    forceOverwriting = forceOverwriting,
    newName = newName,
    explorer = explorer
  )
}

@Suppress("UNCHECKED_CAST")
fun MoveCopyOperation.commonUrls(dataOpsManager: DataOpsManager): Collection<Pair<Requester, ConnectionConfig>> {
  val sourceAttributesPrepared = if (sourceAttributes is RemoteMemberAttributes) {
    sourceAttributes.getLibraryAttributes(dataOpsManager) ?: throw IllegalArgumentException("Cannot find lib attributes")
  } else {
    sourceAttributes
  }
  return (sourceAttributesPrepared as MFRemoteFileAttributes<Requester>)
    .findCommonUrlConnections(destinationAttributes as MFRemoteFileAttributes<Requester>)
}
