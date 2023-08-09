package org.zowe.explorer.dataops.exceptions

import org.zowe.explorer.config.connect.ConnectionConfigBase

/** Raises when no credentials found for connection */
class CredentialsNotFoundForConnection(val connectionConfig: ConnectionConfigBase) : Exception(
  "No username or password found for $connectionConfig"
)
