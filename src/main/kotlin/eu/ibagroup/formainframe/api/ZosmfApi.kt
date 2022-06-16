/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.api

import com.intellij.openapi.application.ApplicationManager
import eu.ibagroup.formainframe.config.connect.ConnectionConfig

interface ZosmfApi {

  companion object {
    @JvmStatic
    val instance: ZosmfApi
      get() = ApplicationManager.getApplication().getService(ZosmfApi::class.java)
  }

  fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api

  fun <Api : Any> getApi(apiClass: Class<out Api>, url: String, isAllowSelfSigned: Boolean, useBytesConverter: Boolean = false): Api

  fun <Api: Any> getApiWithBytesConverter(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api
}

inline fun <reified Api : Any> api(connectionConfig: ConnectionConfig): Api {
  return ZosmfApi.instance.getApi(Api::class.java, connectionConfig)
}

inline fun <reified Api : Any> apiWithBytesConverter(connectionConfig: ConnectionConfig): Api {
  return ZosmfApi.instance.getApiWithBytesConverter(Api::class.java, connectionConfig)
}

inline fun <reified Api : Any> api(url: String, isAllowSelfSigned: Boolean): Api {
  return ZosmfApi.instance.getApi(Api::class.java, url, isAllowSelfSigned)
}
