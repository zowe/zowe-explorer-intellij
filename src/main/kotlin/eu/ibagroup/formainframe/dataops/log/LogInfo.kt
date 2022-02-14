package eu.ibagroup.formainframe.dataops.log

import eu.ibagroup.formainframe.config.connect.ConnectionConfig


interface LogInfo {
  val connectionConfig: ConnectionConfig
}
