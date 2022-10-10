package org.zowe.explorer.dataops.exceptions

import org.zowe.explorer.config.connect.ConnectionConfig

/** Raises when no credentials found for connection */
class CredentialsNotFoundForConnection(val connectionConfig: ConnectionConfig) : Exception(
  "No username or password found for $connectionConfig"
)
