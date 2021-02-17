package eu.ibagroup.formainframe.dataops

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection

interface RemoteInfo {

  val connectionConfig: ConnectionConfig

  val urlConnection: UrlConnection

}