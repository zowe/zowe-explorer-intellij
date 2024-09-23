/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.api

import com.intellij.openapi.components.service
import eu.ibagroup.formainframe.config.connect.ConnectionConfig

/**
 * Interface to represent z/OSMF API.
 */
interface ZosmfApi {

  companion object {
    @JvmStatic
    fun getService(): ZosmfApi = service()
  }

  fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api

  fun <Api : Any> getApi(
    apiClass: Class<out Api>,
    url: String,
    isAllowSelfSigned: Boolean,
    useBytesConverter: Boolean = false
  ): Api

  fun <Api : Any> getApiWithBytesConverter(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api

}

/**
 * Returns API class object from z/OSMF API instance.
 * @param connectionConfig connection configuration to specify the system to work with.
 * @return API class object.
 */
inline fun <reified Api : Any> api(connectionConfig: ConnectionConfig): Api {
  return ZosmfApi.getService().getApi(Api::class.java, connectionConfig)
}

/**
 * Returns API class object with bytes converter from z/OSMF API instance.
 * @param connectionConfig connection configuration to specify the system to work with.
 * @return API class object.
 */
inline fun <reified Api : Any> apiWithBytesConverter(connectionConfig: ConnectionConfig): Api {
  return ZosmfApi.getService().getApiWithBytesConverter(Api::class.java, connectionConfig)
}

/**
 * Returns API class object from z/OSMF API instance.
 * @param url url address of the remote system.
 * @param isAllowSelfSigned whether to allow self-signed certificates.
 * @return API class object.
 */
inline fun <reified Api : Any> api(url: String, isAllowSelfSigned: Boolean): Api {
  return ZosmfApi.getService().getApi(Api::class.java, url, isAllowSelfSigned)
}
