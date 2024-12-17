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

package org.zowe.explorer.config.connect

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.dataops.operations.TsoOperationMode
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.testutils.testServiceImpl.TestZosmfApiImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.tso.getTsoMessageQueue
import org.zowe.kotlinsdk.*
import org.zowe.kotlinsdk.annotations.ZVersion
import retrofit2.Call
import retrofit2.Response

class ConnectUtilsTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("config/connect/connectUtils") {
    // z/OS > 2.3 call setup
    fun setupTsoEnhancedCall(
      tsoResultBody: MutableList<TsoCmdResult>,
      shouldThrowException: Boolean,
      success: Boolean
    ) {
      val responseBody = TsoCmdResponse(cmdResponse = tsoResultBody)
      val tsoApi = mockk<TsoApi>()
      val call = mockk<Call<TsoCmdResponse>>()
      val response = mockk<Response<TsoCmdResponse>>()

      val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
      zosmfApi.testInstance = object : TestZosmfApiImpl() {
        override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
          return if (apiClass == TsoApi::class.java) {
            tsoApi as Api
          } else {
            super.getApi(apiClass, connectionConfig)
          }
        }
      }

      every { tsoApi.executeTsoCommand(any(), any(), any()) } returns call
      every { call.execute() } answers {
        if (shouldThrowException) throw IllegalStateException("Test call failed") else response
      }
      every { response.isSuccessful } returns success
      every { response.body() } returns responseBody
    }

    val connectionConfigZOS23 = ConnectionConfig()
    connectionConfigZOS23.zVersion = ZVersion.ZOS_2_3
    val connectionConfigZOS24 = ConnectionConfig()
    connectionConfigZOS24.zVersion = ZVersion.ZOS_2_4

    val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl

    beforeEach {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          val tsoResponse = TsoResponse(
            servletKey = "servletKey",
            tsoData = listOf(TsoData())
          )
          if ((operation as TsoOperation).mode == TsoOperationMode.SEND_MESSAGE) {
            tsoResponse.tsoData = listOf(
              TsoData(tsoMessage = MessageType("", "ZOSMFAD  "))
            )
          }
          @Suppress("UNCHECKED_CAST")
          return tsoResponse as R
        }
      }

      mockkStatic("org.zowe.explorer.tso.TSOWindowFactoryKt")
      every { getTsoMessageQueue(any()) } answers {
        TsoResponse(
          tsoData = listOf(
            TsoData(tsoPrompt = MessageType(""))
          )
        )
      }

      mockkObject(CredentialService.Companion)
      every { CredentialService.getUsername(any<ConnectionConfig>()) } returns "ZOSMF"
    }
    afterEach {
      unmockkAll()
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
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          val tsoResponse = TsoResponse(
            servletKey = "servletKey",
            tsoData = listOf(TsoData())
          )
          if ((operation as TsoOperation).mode == TsoOperationMode.SEND_MESSAGE) {
            tsoResponse.tsoData = listOf(
              TsoData(tsoMessage = MessageType("", ""))
            )
          }
          @Suppress("UNCHECKED_CAST")
          return tsoResponse as R
        }
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }


    should("return empty owner if TSO request returns READY") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          val tsoResponse = TsoResponse(
            servletKey = "servletKey",
            tsoData = listOf(TsoData())
          )
          if ((operation as TsoOperation).mode == TsoOperationMode.SEND_MESSAGE) {
            tsoResponse.tsoData = listOf(
              TsoData(tsoMessage = MessageType("", "READY "))
            )
          }
          @Suppress("UNCHECKED_CAST")
          return tsoResponse as R
        }
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }

    should("return empty owner if TSO request returns error message in TSO data") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          val tsoResponse = TsoResponse(
            servletKey = "servletKey",
            tsoData = listOf(TsoData())
          )
          if ((operation as TsoOperation).mode == TsoOperationMode.SEND_MESSAGE) {
            tsoResponse.tsoData = listOf(
              TsoData(tsoMessage = MessageType("", "OSHELL RC = 65210"))
            )
          }
          @Suppress("UNCHECKED_CAST")
          return tsoResponse as R
        }
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }

    should("return empty owner by TSO request if servlet key is null") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          @Suppress("UNCHECKED_CAST")
          return TsoResponse() as R
        }
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }
    should("return empty owner by TSO request if servlet key is empty") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          @Suppress("UNCHECKED_CAST")
          return TsoResponse(servletKey = "") as R
        }
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }
    should("return empty owner by TSO request if request fails") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          val tsoResponse = TsoResponse(
            servletKey = "servletKey",
            tsoData = listOf(TsoData())
          )
          if ((operation as TsoOperation).mode == TsoOperationMode.SEND_MESSAGE) {
            throw Exception("Failed to send message")
          }
          @Suppress("UNCHECKED_CAST")
          return tsoResponse as R
        }
      }

      val actual = whoAmI(connectionConfigZOS23)

      assertSoftly { actual shouldBe "" }
    }
  }
})