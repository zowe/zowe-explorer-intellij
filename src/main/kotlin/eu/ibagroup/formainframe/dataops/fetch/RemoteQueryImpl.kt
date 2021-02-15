package eu.ibagroup.formainframe.dataops.fetch

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection

data class RemoteQueryImpl<R>(
  override val request: R,
  override val connectionConfig: ConnectionConfig,
  override val urlConnection: UrlConnection
) : RemoteQuery<R>