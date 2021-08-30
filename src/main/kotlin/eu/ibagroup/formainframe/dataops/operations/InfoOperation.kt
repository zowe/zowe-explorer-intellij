package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.r2z.InfoResponse

class InfoOperation(var url: String, val isAllowSelfSigned: Boolean) : Operation<InfoResponse> {
  override val resultClass = InfoResponse::class.java
}