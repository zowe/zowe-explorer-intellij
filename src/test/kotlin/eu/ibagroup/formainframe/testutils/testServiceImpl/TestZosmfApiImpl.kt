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

package eu.ibagroup.formainframe.testutils.testServiceImpl

import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.connect.ConnectionConfig

open class TestZosmfApiImpl : ZosmfApi {
  var testInstance = object : ZosmfApi {
    override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
      TODO("Not yet implemented")
    }

    override fun <Api : Any> getApi(
      apiClass: Class<out Api>,
      url: String,
      isAllowSelfSigned: Boolean,
      useBytesConverter: Boolean
    ): Api {
      TODO("Not yet implemented")
    }

    override fun <Api : Any> getApiWithBytesConverter(
      apiClass: Class<out Api>,
      connectionConfig: ConnectionConfig
    ): Api {
      TODO("Not yet implemented")
    }

  }

  override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
    return this.testInstance.getApi(apiClass, connectionConfig)
  }

  override fun <Api : Any> getApi(
    apiClass: Class<out Api>,
    url: String,
    isAllowSelfSigned: Boolean,
    useBytesConverter: Boolean
  ): Api {
    return this.testInstance.getApi(apiClass, url, isAllowSelfSigned, useBytesConverter)
  }

  override fun <Api : Any> getApiWithBytesConverter(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
    return this.testInstance.getApiWithBytesConverter(apiClass, connectionConfig)
  }
}