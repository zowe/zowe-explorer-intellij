package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.*

class MoveCopyOperation(
  val source: VirtualFile,
  val sourceAttributes: VFileInfoAttributes?,
  val destination: VirtualFile,
  val destinationAttributes: VFileInfoAttributes?,
  val isMove: Boolean,
  val forceOverwriting: Boolean,
  val newName: String?
) : UnitOperation {
  constructor(
    source: VirtualFile,
    destination: VirtualFile,
    isMove: Boolean,
    forceOverwriting: Boolean,
    newName: String?,
    dataOpsManager: DataOpsManager
  ) : this(
    source = source,
    sourceAttributes = dataOpsManager.tryToGetAttributes(source),
    destination = destination,
    destinationAttributes = dataOpsManager.tryToGetAttributes(destination),
    isMove = isMove,
    forceOverwriting = forceOverwriting,
    newName = newName
  )
}

@Suppress("UNCHECKED_CAST")
fun MoveCopyOperation.commonUrls(dataOpsManager: DataOpsManager): Collection<Pair<Requester, UrlConnection>> {
  val sourceAttributesPrepared = if (sourceAttributes is RemoteMemberAttributes) {
    sourceAttributes.getLibraryAttributes(dataOpsManager) ?: throw IllegalArgumentException("Cannot find lib attributes")
  } else {
    sourceAttributes
  }
  return (sourceAttributesPrepared as MFRemoteFileAttributes<Requester>)
    .findCommonUrlConnections(destinationAttributes as MFRemoteFileAttributes<Requester>)
}