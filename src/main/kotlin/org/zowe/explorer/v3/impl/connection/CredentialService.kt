/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.connection

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import org.zowe.explorer.v3.impl.teamconfig.ConfigType

/**
 * Credentials resolved from the Zowe profile hierarchy.
 * Each field is `null` when no profile in the chain provides a value
 */
data class ResolvedCredentials(
  val user: String?,
  val password: String?
)

/**
 * Application-level service that lazily resolves credentials (user/password)
 * from `zowe.config.json` by walking the profile hierarchy via [ProfileChainResolver].
 *
 * Separated from [ConnectionService] because credentials should only be resolved
 * at the moment they are actually needed (e.g. right before an API call),
 * not when the connection metadata is loaded
 */
@Service
class CredentialService {
  companion object {
    fun getService() = service<CredentialService>()
  }

  /**
   * Resolves user and password for the given [connectionProfilePath]
   * by walking the profile hierarchy. Secure properties are read from the OS secure store
   * @param configType the active config type (local/global)
   * @param projectBasePath the project base path for local config resolution
   * @param connectionProfilePath dot-separated profile path (e.g. "lpar1.zosmf")
   * @return the resolved credentials
   */
  fun resolveCredentials(
    configType: ConfigType,
    projectBasePath: String?,
    connectionProfilePath: String
  ): ResolvedCredentials {
    val ctx = ProfileChainResolver.resolve(configType, projectBasePath, connectionProfilePath)
      ?: return ResolvedCredentials(null, null)

    return ResolvedCredentials(
      user = ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, "user"),
      password = ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, "password")
    )
  }

  /**
   * Resolves only the user for the given [connectionProfilePath]
   * @return the resolved user, or `null` if not found in any profile
   */
  fun resolveUser(
    configType: ConfigType,
    projectBasePath: String?,
    connectionProfilePath: String
  ): String? {
    val ctx = ProfileChainResolver.resolve(configType, projectBasePath, connectionProfilePath)
      ?: return null
    return ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, "user")
  }

  /**
   * Resolves only the password for the given [connectionProfilePath]
   * @return the resolved password, or `null` if not found in any profile
   */
  fun resolvePassword(
    configType: ConfigType,
    projectBasePath: String?,
    connectionProfilePath: String
  ): String? {
    val ctx = ProfileChainResolver.resolve(configType, projectBasePath, connectionProfilePath)
      ?: return null
    return ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, "password")
  }
}
