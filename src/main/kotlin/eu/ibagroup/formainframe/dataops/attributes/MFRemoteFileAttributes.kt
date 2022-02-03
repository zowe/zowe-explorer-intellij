package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.crudable.getByForeignKey

interface MFRemoteFileAttributes<R : Requester> : FileAttributes {

  val url: String

  val requesters: MutableList<R>

}

interface Requester {
  val connectionConfig: ConnectionConfig
}

inline fun <reified R : Requester> MFRemoteFileAttributes<R>.findCommonUrlConnections(other: MFRemoteFileAttributes<R>)
  : Collection<Pair<R, ConnectionConfig>> {
  val thisRequestersWithUrlConnection = requesters.map {
    Pair(it, it.connectionConfig)
  }
  val otherUrlConnections = other.requesters.map {
    it.connectionConfig
  }
  return thisRequestersWithUrlConnection.filter { pair ->
    otherUrlConnections.any {
      pair.second.url == it.url && pair.second.isAllowSelfSigned == it.isAllowSelfSigned && pair.second.codePage == it.codePage
    }
  }
}
