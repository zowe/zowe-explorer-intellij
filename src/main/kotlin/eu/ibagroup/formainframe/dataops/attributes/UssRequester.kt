package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.r2z.FileMode

data class UssRequester(
  override val user: String,
  ) : Requester