/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.state.credentials.cache

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.utils.clone
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.credentials.CredentialsState

@Service
class CredentialsCacheService {
  companion object {
    fun getService(): CredentialsCacheService = service()
  }

  private val stateLock = Any()

  private var cacheState = CredentialsState()
    get() = synchronized(stateLock) { field }
    private set(value) = synchronized(stateLock) { field = value }

  private var storageState = CredentialsState()

  fun getCredentialsFromCache(configUuid: String): Credentials? {
    return cacheState.credentialsList.firstOrNull { it.configUuid == configUuid }
  }

  fun addCredentialsToCache(credentials: Credentials) {
    cacheState.credentialsList.add(credentials)
  }

  fun updateCredentialsInCache(credentials: Credentials) {
    cacheState.credentialsList.indexOfFirst { it.configUuid == credentials.configUuid }.let { index ->
      cacheState.credentialsList[index] = credentials
    }
  }

  fun deleteCredentialsFromCache(credentials: Credentials) {
    cacheState.credentialsList.indexOfFirst { it.configUuid == credentials.configUuid }.let { index ->
      cacheState.credentialsList.removeAt(index)
    }
  }

  fun isCacheModified(): Boolean {
    return synchronized(stateLock) {
      val isModified = storageState.credentialsList.sortedBy { it.configUuid } !=
        cacheState.credentialsList.sortedBy { it.configUuid }
      isModified
    }
  }

  fun saveCacheToStorage() {
    synchronized(stateLock) {
      if (isCacheModified()) {
        val credentialsToDelete = storageState.credentialsList
          .filter { oldCredentials ->
            cacheState.credentialsList.none { newCredentials ->
              oldCredentials.configUuid == newCredentials.configUuid
            }
          }
        credentialsToDelete.forEach { credentials ->
          CredentialService.getService().clearCredentials(credentials.configUuid)
        }

        val credentialsToAdd = cacheState.credentialsList
          .filter { newCredentials ->
            storageState.credentialsList.none { oldCredentials ->
              oldCredentials.configUuid == newCredentials.configUuid
            }
          }
        credentialsToAdd.forEach { credentials ->
          CredentialService.getService().setCredentials(credentials.configUuid, credentials.username, credentials.password)
        }

        val credentialsToUpdate = cacheState.credentialsList
          .filter { newCredentials ->
            storageState.credentialsList.any { oldCredentials ->
              oldCredentials.configUuid == newCredentials.configUuid && oldCredentials != newCredentials
            }
          }
        credentialsToUpdate.forEach { credentials ->
          CredentialService.getService().setCredentials(credentials.configUuid, credentials.username, credentials.password)
        }
        storageState.credentialsList.clear()
        cacheState.credentialsList.forEach {
          storageState.credentialsList.add(it.clone())
        }
      }
    }
  }

  fun reloadCredentialsFromStorage(configType: ConfigType) {
    synchronized(stateLock) {
      val credentialsList = ConfigCacheService.getService()
        .getConfigsFromCache(configType).toList()
        .map { config ->
          Credentials(
            config.uuid,
            CredentialService.getService().getUsernameByKey(config.uuid) ?: "",
            CredentialService.getService().getPasswordByKey(config.uuid) ?: charArrayOf()
          )
        }
      storageState.credentialsList.clear()
      storageState.credentialsList.addAll(credentialsList)
      cacheState.credentialsList.clear()
      credentialsList.forEach { credentials ->
        cacheState.credentialsList.add(credentials.clone())
      }
    }
  }

  fun clearCache() {
    synchronized(stateLock) {
      storageState.credentialsList.clear()
      cacheState.credentialsList.clear()
    }
  }

}