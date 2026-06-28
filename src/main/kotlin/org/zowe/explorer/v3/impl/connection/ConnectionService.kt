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
 * Resolved connection properties merged from the Zowe profile hierarchy.
 * Does not include credentials — use [CredentialService] to resolve them lazily.
 * Each field is `null` when no profile in the chain provides a value
 */
data class ResolvedConnection(
  val host: String?,
  val port: Int?,
  val protocol: String?,
  val basePath: String?,
  val rejectUnauthorized: Boolean?
)

/**
 * Application-level service that resolves non-credential connection properties
 * from `zowe.config.json` by walking the profile hierarchy via [ProfileChainResolver].
 * Credentials are resolved separately by [CredentialService]
 */
@Service
class ConnectionService {
  companion object {
    fun getService() = service<ConnectionService>()
  }

  /**
   * Resolves a [ResolvedConnection] for the given [connectionProfilePath]
   * by merging properties from the profile hierarchy
   * @param configType the active config type (local/global)
   * @param projectBasePath the project base path for local config resolution
   * @param connectionProfilePath dot-separated profile path (e.g. "lpar1.zosmf")
   * @return the merged connection properties
   */
  fun resolveConnection(
    configType: ConfigType,
    projectBasePath: String?,
    connectionProfilePath: String
  ): ResolvedConnection {
    val ctx = ProfileChainResolver.resolve(configType, projectBasePath, connectionProfilePath)
      ?: return ResolvedConnection(null, null, null, null, null)

    return ResolvedConnection(
      host = ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, "host"),
      port = ProfileChainResolver.resolveInt(ctx.chain, ctx.configFilePath, "port"),
      protocol = ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, "protocol"),
      basePath = ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, "basePath"),
      rejectUnauthorized = ProfileChainResolver.resolveBoolean(ctx.chain, ctx.configFilePath, "rejectUnauthorized")
    )
  }

  /**
   * Resolves a single string property by walking the profile chain
   * @param configType the active config type
   * @param projectBasePath the project base path
   * @param connectionProfilePath dot-separated profile path
   * @param propertyName the property to resolve
   * @return the resolved value, or `null` if not found in any profile
   */
  fun resolveProperty(
    configType: ConfigType,
    projectBasePath: String?,
    connectionProfilePath: String,
    propertyName: String
  ): String? {
    val ctx = ProfileChainResolver.resolve(configType, projectBasePath, connectionProfilePath)
      ?: return null
    return ProfileChainResolver.resolveString(ctx.chain, ctx.configFilePath, propertyName)
  }
}
