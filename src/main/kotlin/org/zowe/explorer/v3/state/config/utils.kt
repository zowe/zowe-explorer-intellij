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

package org.zowe.explorer.v3.state.config

import okhttp3.HttpUrl
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig

fun normalizeEncodedPath(path: String? = null): String {
  return if (path.isNullOrBlank()) {
    "/"
  } else {
    val trimmed = path.trimStart('/').trimEnd('/')
    "/$trimmed/"
  }
}

fun getUrl(scheme: String, host: String, port: Int, path: String? = null): String {
  return HttpUrl.Builder()
    .scheme(scheme)
    .host(host)
    .port(port)
    .encodedPath(normalizeEncodedPath(path))
    .build()
    .toString()
}

fun getUrl(connectionConfig: ConnectionConfig): String {
  return if (connectionConfig.host.isNotBlank() && connectionConfig.port > 0) {
    getUrl(
      connectionConfig.scheme.scheme,
      connectionConfig.host,
      connectionConfig.port
    )
  } else ""
}

fun getUrlWithBasePath(httpConnectionConfig: HttpConnectionConfig): String {
  return if (httpConnectionConfig.host.isNotBlank() && httpConnectionConfig.port > 0) {
    getUrl(
      httpConnectionConfig.scheme.scheme,
      httpConnectionConfig.host,
      httpConnectionConfig.port,
      httpConnectionConfig.basePath
    )
  } else ""
}
