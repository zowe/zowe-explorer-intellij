package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.utils.nullIfBlank

data class MaskedRequester(
  override val user: String,
  val queryMask: DSMask,
) : Requester {
  val queryVolser: String?
    get() = queryMask.volser.nullIfBlank()
}