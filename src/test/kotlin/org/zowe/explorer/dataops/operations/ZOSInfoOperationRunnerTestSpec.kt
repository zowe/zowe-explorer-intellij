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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.InfoAPI
import org.zowe.kotlinsdk.InfoResponse
import retrofit2.Response
import kotlin.reflect.KFunction

class ZOSInfoOperationRunnerTestSpec : AppInitShouldSpec("dataops/operations/ZOSInfoOperationRunner", {
  context("all functions") {
    val zosInfoOperationRunner = ZOSInfoOperationRunner()

    val progressIndicatorMockk = mockk<ProgressIndicator>()
    val responseMockk = mockk<Response<InfoResponse>>()

    val zosmfApi = ZosmfApi.getService()
    every { zosmfApi.getApi(InfoAPI::class.java, any<ConnectionConfig>()) } returns mockk {
      every { getSystemInfo() } returns mockk {
        every { execute() } returns responseMockk
      }
    }

    val operationMockk = mockk<ZOSInfoOperation> {
      every { connectionConfig } returns mockk()
    }

    val callExceptionRef: (Response<*>, String) -> CallException = ::CallException
    mockkStatic(callExceptionRef as KFunction<*>)

    beforeEach {
      every { responseMockk.isSuccessful } returns true
      every { responseMockk.body() } returns null
    }

    should("perform Info operation with successful response") {
      val expected = InfoResponse(zosVersion = "2.3", zosmfHostname = "host", zosmfPort = "port")

      every { responseMockk.body() } returns expected

      val actual = zosInfoOperationRunner.run(operationMockk, progressIndicatorMockk)

      assertSoftly { actual shouldBe expected }
    }

    should("perform Info operation with unsuccessful response") {
      val expected = CallException(500, "An internal error has occurred")

      every { responseMockk.isSuccessful } returns false
      every { CallException(responseMockk, any()) } returns expected

      var actual: Throwable? = null
      runCatching {
        zosInfoOperationRunner.run(operationMockk, progressIndicatorMockk)
      }.onFailure {
        actual = it
      }

      assertSoftly { actual shouldBe expected }
    }

    should("perform Info operation when response body is null") {
      val expected = CallException(500, "Cannot parse z/OSMF info request body")

      every { CallException(responseMockk, any()) } returns expected

      var actual: Throwable? = null
      runCatching {
        zosInfoOperationRunner.run(operationMockk, progressIndicatorMockk)
      }.onFailure {
        actual = it
      }

      assertSoftly { actual shouldBe expected }
    }
  }
})
