package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.r2z.InfoResponse

/**
 * Base class which represents an operation to be performed against system where z/OSMF instance is running
 * @param connectionConfig represents a connection config object
 */
class ZOSInfoOperation(var connectionConfig: ConnectionConfig) : Operation<InfoResponse> {
  override val resultClass = InfoResponse::class.java
}