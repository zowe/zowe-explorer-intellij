package eu.ibagroup.formainframe.dataops.exceptions

import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase

/** Raises when no credentials found for connection */
class CredentialsNotFoundForConnection(val connectionConfig: ConnectionConfigBase) : Exception(
  "No username or password found for $connectionConfig"
)