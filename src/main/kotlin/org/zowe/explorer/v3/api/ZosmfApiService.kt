/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.net.ssl.CertificateManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.zowe.kotlinsdk.core.WrapperType
import org.zowe.kotlinsdk.core.datasets.api.DatasetsAPI
import org.zowe.kotlinsdk.core.files.api.FilesAPI
import org.zowe.kotlinsdk.providers.zowe.HttpRequestRunner
import org.zowe.kotlinsdk.providers.zowe.ZoweAPIProvider

// TODO: doc
@Service
class ZosmfApiService {
  companion object {
    fun getService(): ZosmfApiService = service()
  }

  private val zosmfClient by lazy {
    HttpClient(CIO) {
      install(ContentNegotiation.Plugin) {
        json(
          Json {
            ignoreUnknownKeys = true
            prettyPrint = true
          }
        )
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 30000
      }
      engine {
        https {
          trustManager = CertificateManager.getInstance().trustManager
        }
      }
    }
  }

  private val zoweAPIProvider by lazy { ZoweAPIProvider(listOf(HttpRequestRunner(zosmfClient))) }

  val datasets: DatasetsAPI by lazy { zoweAPIProvider.getApi(WrapperType.ZOSMF, DatasetsAPI::class.java) }
  val files: FilesAPI by lazy { zoweAPIProvider.getApi(WrapperType.ZOSMF, FilesAPI::class.java) }
}