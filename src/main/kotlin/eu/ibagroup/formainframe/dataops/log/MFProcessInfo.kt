package eu.ibagroup.formainframe.dataops.log

import eu.ibagroup.formainframe.config.connect.ConnectionConfig

/**
 * Base interface for implementing mainframe process unique information like id or name.
 * @author Valentine Krus
 */
interface MFProcessInfo {
  val connectionConfig: ConnectionConfig
}
