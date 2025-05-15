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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.config.connect

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.dataops.operations.TsoOperationMode
import org.zowe.explorer.tso.getTsoMessageQueue
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.*
import org.zowe.kotlinsdk.annotations.ZVersion
import retrofit2.Call
import retrofit2.Response

class ConnectUtilsTestSpec : AppInitShouldSpec("config/connect/connectUtils", {
  context("all functions") {
    // z/OS > 2.3 call setup
    fun setupTsoEnhancedCall(
      tsoResultBody: MutableList<TsoCmdResult>,
      shouldThrowException: Boolean,
      success: Boolean
    ) {
      val responseBody = TsoCmdResponse(cmdResponse = tsoResultBody)
      val response = mockk<Response<TsoCmdResponse>> {
        every { isSuccessful } returns success
        every { body() } returns responseBody
      }
      val call = mockk<Call<TsoCmdResponse>> {
        every { execute() } answers {
          if (shouldThrowException) throw IllegalStateException("Test call failed") else response
        }
      }
      val tsoApi = mockk<TsoApi> {
        every { executeTsoCommand(any(), any(), any()) } returns call
      }

      val zosmfApi = ZosmfApi.getService()
      every {
        zosmfApi.getApi(any<Class<*>>(), any<ConnectionConfig>())
      } answers {
        val apiClass = firstArg<Class<*>>()
        if (apiClass == TsoApi::class.java) {
          tsoApi
        } else {
          fail("Unknown API class: $apiClass")
        }
      }
    }

    val connectionConfigZOS23 = ConnectionConfig()
    connectionConfigZOS23.zVersion = ZVersion.ZOS_2_3
    val connectionConfigZOS24 = ConnectionConfig()
    connectionConfigZOS24.zVersion = ZVersion.ZOS_2_4

    lateinit var dataOpsManagerService: DataOpsManager

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "ZOSMF"
    every { credentialService.getPasswordByKey(any<String>()) } returns "TEST".toCharArray()

    beforeEach {
      dataOpsManagerService = DataOpsManager.getService()
      every {
        dataOpsManagerService.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
      } answers {
        val operation = firstArg<TsoOperation>()
        val tsoResponse = TsoResponse(servletKey = "servletKey", tsoData = listOf(TsoData()))
        if (operation.mode == TsoOperationMode.SEND_MESSAGE) {
          tsoResponse.tsoData = listOf(
            TsoData(tsoMessage = MessageType("", "ZOSMFAD  "))
          )
        }
        tsoResponse
      }

      mockkStatic("org.zowe.explorer.tso.TSOWindowFactoryKt")
      every {
        getTsoMessageQueue(any())
      } answers {
        TsoResponse(
          tsoData = listOf(TsoData(tsoPrompt = MessageType("")))
        )
      }
    }

    // whoAmI
    should("get the owner by TSO request if z/OS version = 2.4") {
      val tsoResultBody = mutableListOf(TsoCmdResult(message = "ZOSMFAD"))
      setupTsoEnhancedCall(tsoResultBody, success = true, shouldThrowException = false)

      val actual = whoAmI(connectionConfigZOS24)

      assertSoftly { actual shouldBe "ZOSMFAD" }
    }

    should("return empty owner by TSO request if z/OS version = 2.4 and owner cannot be retrieved") {
      val tsoResultBody = mutableListOf(
        TsoCmdResult(message = ""),
        TsoCmdResult(message = "OSHELL RC = 2020"),
        TsoCmdResult(message = "READY ")
      )
      setupTsoEnhancedCall(tsoResultBody, success = true, shouldThrowException = false)

      val actual = whoAmI(connectionConfigZOS24)

      assertSoftly { actual shouldBe "" }
    }

    should("return empty owner by TSO request if z/OS version = 2.4 and tso request fails") {
      setupTsoEnhancedCall(mutableListOf(), success = false, shouldThrowException = true)

      val actual = whoAmI(connectionConfigZOS24)

      assertSoftly { actual shouldBe "" }
    }

    should("get the owner by TSO request if z/OS version = 2.3") {
      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "ZOSMFAD" }
    }

    should("return empty owner if TSO request returns empty data") {
      every {
        dataOpsManagerService.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
      } answers {
        val operation = firstArg<TsoOperation>()
        val tsoResponse = TsoResponse(servletKey = "servletKey", tsoData = listOf(TsoData()))
        if (operation.mode == TsoOperationMode.SEND_MESSAGE) {
          tsoResponse.tsoData = listOf(
            TsoData(tsoMessage = MessageType("", ""))
          )
        }
        tsoResponse
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }


    should("return empty owner if TSO request returns READY") {
      every {
        dataOpsManagerService.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
      } answers {
        val operation = firstArg<TsoOperation>()
        val tsoResponse = TsoResponse(servletKey = "servletKey", tsoData = listOf(TsoData()))
        if (operation.mode == TsoOperationMode.SEND_MESSAGE) {
          tsoResponse.tsoData = listOf(
            TsoData(tsoMessage = MessageType("", "READY "))
          )
        }
        tsoResponse
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }

    should("return empty owner if TSO request returns error message in TSO data") {
      every {
        dataOpsManagerService.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
      } answers {
        val operation = firstArg<TsoOperation>()
        val tsoResponse = TsoResponse(servletKey = "servletKey", tsoData = listOf(TsoData()))
        if (operation.mode == TsoOperationMode.SEND_MESSAGE) {
          tsoResponse.tsoData = listOf(
            TsoData(tsoMessage = MessageType("", "OSHELL RC = 65210"))
          )
        }
        tsoResponse
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }

    should("return empty owner by TSO request if servlet key is null") {
      every {
        dataOpsManagerService.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
      } returns TsoResponse()

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }
    should("return empty owner by TSO request if servlet key is empty") {
      every {
        dataOpsManagerService.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
      } returns TsoResponse(servletKey = "")

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }
    should("return empty owner by TSO request if request fails") {
      every {
        dataOpsManagerService.performOperation(any<TsoOperation>(), any<ProgressIndicator>())
      } answers {
        val operation = firstArg<TsoOperation>()
        val tsoResponse = TsoResponse(servletKey = "servletKey", tsoData = listOf(TsoData()))
        if (operation.mode == TsoOperationMode.SEND_MESSAGE) {
          throw Exception("Failed to send message")
        }
        tsoResponse
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }
  }
})
