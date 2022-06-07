package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.r2z.SystemsResponse

class InfoOperation(var connectionConfig: ConnectionConfig) : Operation<SystemsResponse> {
  override val resultClass = SystemsResponse::class.java
}