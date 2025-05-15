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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.utils.cancelByIndicator
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.ChangeOwner
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.FilePath
import retrofit2.Response

class UssChangeOwnerTestSpec : AppInitShouldSpec("dataops/operations/UssChangeOwnerTestSpec", {
  context("UssChangeOwnerOperationRunner common spec") {
    val dataApi = mockk<DataAPI>()
    val zosmfApi = ZosmfApi.getService()
    every { zosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns dataApi

    val classUnderTest = spyk(UssChangeOwner())
    val operation = mockk<UssChangeOwnerOperation>()

    context("canRun") {
      should("returnTrue_whenCanRun_givenRemoteMemberAttributes") {
        val canRun = classUnderTest.canRun(operation)

        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnTrue_whenCanRun_givenRemoteDatasetAttributes") {
        val canRun = classUnderTest.canRun(operation)

        assertSoftly {
          canRun shouldBe true
        }
      }

      should("returnTrue_whenCanRun_givenRemoteUssAttributes") {
        val canRun = classUnderTest.canRun(operation)

        assertSoftly {
          canRun shouldBe true
        }
      }
    }

    context("run operation") {
      val credentialService = CredentialService.getService()
      every { credentialService.getUsernameByKey(any<String>()) } returns "test"
      every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

      val progressIndicator = mockk<ProgressIndicator> {
        every { checkCanceled() } just Runs
      }
      val changeOwnerBodyReqParams = ChangeOwner(owner = "owner", group = "group")
      val ussFilePath = "attributes/path"
      val ussChangeOwnerParams = UssChangeOwnerParams(changeOwnerBodyReqParams, ussFilePath)
      val connectionConfigMock = mockk<ConnectionConfig> {
        every { uuid } returns "00000000"
      }
      every { operation.connectionConfig } returns connectionConfigMock
      every { operation.request } returns ussChangeOwnerParams
      val apiResponse = mockk<Response<Void>>()
      every {
        dataApi.changeFileOwner(any(), any(), any(), any()).cancelByIndicator(progressIndicator).execute()
      } returns apiResponse

      should("successfully run changeFileOwner operation") {
        every { apiResponse.isSuccessful } returns true

        classUnderTest.run(operation, progressIndicator)

        verify(exactly = 1) {
          dataApi.changeFileOwner(any<String>(), null, changeOwnerBodyReqParams, FilePath(ussFilePath))
        }
      }

      should("failed run changeFileOwner operation") {
        every { apiResponse.isSuccessful } returns false
        every { apiResponse.code() } returns 500
        val exception = shouldThrowExactly<CallException> {
          classUnderTest.run(operation, progressIndicator)
        }
        assertSoftly {
          exception.message shouldBe "Cannot change file owner on attributes/path\nCode: 500"
        }
      }
    }
  }
})
