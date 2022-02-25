package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialogState
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.r2z.SystemsResponse

class InfoOperation(var state: ConnectionDialogState) : Operation<SystemsResponse> {
  override val resultClass = SystemsResponse::class.java
}