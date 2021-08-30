package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.config.connect.ConnectionConfig

interface ExplorerUnit {

  val name: String

  val uuid: String

  val connectionConfig: ConnectionConfig?

  val explorer: Explorer

}