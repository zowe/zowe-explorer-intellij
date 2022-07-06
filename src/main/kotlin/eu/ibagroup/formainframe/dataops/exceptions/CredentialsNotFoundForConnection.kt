package eu.ibagroup.formainframe.dataops.exceptions

import eu.ibagroup.formainframe.config.connect.ConnectionConfig

/** Raises when no credentials found for connection */
class CredentialsNotFoundForConnection(val connectionConfig: ConnectionConfig) : Exception(
  "No username or password found for $connectionConfig"
)