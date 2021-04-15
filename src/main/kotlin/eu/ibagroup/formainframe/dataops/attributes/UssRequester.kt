package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.config.connect.ConnectionConfig

data class UssRequester(
  override val connectionConfig: ConnectionConfig
) : Requester