package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.utils.nullIfBlank

data class MaskedRequester(
  override val connectionConfig: ConnectionConfig,
  val queryMask: DSMask,
) : Requester {
  val queryVolser: String?
    get() = queryMask.volser.nullIfBlank()
}