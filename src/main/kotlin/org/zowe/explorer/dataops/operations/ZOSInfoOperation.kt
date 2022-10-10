package org.zowe.explorer.dataops.operations

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.Operation
import org.zowe.kotlinsdk.InfoResponse

/**
 * Base class which represents an operation to be performed against system where z/OSMF instance is running
 * @param connectionConfig represents a connection config object
 */
class ZOSInfoOperation(var connectionConfig: ConnectionConfig) : Operation<InfoResponse> {
  override val resultClass = InfoResponse::class.java
}
