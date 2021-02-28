package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.utils.crudable.getByForeignKey

interface MFRemoteFileAttributes<R : Requester> : VFileInfoAttributes {

  val url: String

  val requesters: MutableList<R>

}

interface Requester {
  val connectionConfig: ConnectionConfig
}

inline fun <reified R : Requester> MFRemoteFileAttributes<R>.findCommonUrlConnections(other: MFRemoteFileAttributes<R>)
  : Collection<Pair<R, UrlConnection>> {
  val thisRequestersWithUrlConnection = requesters.mapNotNull {
    val urlConnection: UrlConnection? = configCrudable.getByForeignKey(it)
    if (urlConnection != null) {
      Pair(it, urlConnection)
    } else null
  }
  val otherRequestersWithUrlConnection = requesters.mapNotNull<R, UrlConnection> {
    configCrudable.getByForeignKey(it)
  }
  return thisRequestersWithUrlConnection.filter { pair ->
    otherRequestersWithUrlConnection.any {
      pair.second.url == it.url && pair.second.isAllowSelfSigned == it.isAllowSelfSigned
    }
  }
}
