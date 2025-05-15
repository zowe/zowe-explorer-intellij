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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.exceptions.CallException
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.MembersList
import retrofit2.Call
import retrofit2.Response

class MemberAllocatorTestSpec : AppInitShouldSpec("dataops/operations/MemberAllocator", {
  context("MemberAllocator test spec") {
    val classUnderTest = spyk<MemberAllocator>()

    context("run operation") {
      val progressIndicator = mockk<ProgressIndicator> {
        every { checkCanceled() } just Runs
      }

      val credentialService = CredentialService.getService()
      every { credentialService.getUsernameByKey(any<String>()) } returns "test"
      every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

      val connectionConfigMock = mockk<ConnectionConfig> {
        every { name } returns "test_connection"
        every { uuid } returns "test_uuid"
      }
      val memberAllocationParams = mockk<MemberAllocationParams> {
        every { memberName } returns "test"
        every { datasetName } returns "ZOSMFAD.TEST"
      }
      val memberAllocationOperation = mockk<MemberAllocationOperation> {
        every { request } returns memberAllocationParams
        every { connectionConfig } returns connectionConfigMock
      }

      val listCall = mockk<Call<MembersList>>()
      val listResponse = mockk<Response<MembersList>>()
      val writeCall = mockk<Call<Void>>()
      val writeResponse = mockk<Response<Void>>()

      val dataApi = mockk<DataAPI> {
        every { listDatasetMembers(any(), any(), any(), any(), any(), any(), any()) } returns listCall
        every {
          writeToDatasetMember(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
          )
        } returns writeCall
      }

      val zosmfApi = ZosmfApi.getService()
      every { zosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns dataApi
      every { zosmfApi.getApiWithBytesConverter(DataAPI::class.java, any<ConnectionConfig>()) } returns dataApi

      val membersList = mockk<MembersList>()
      val member1 = mockk<Member>()
      val member2 = mockk<Member>()

      afterEach {
        clearMocks(dataApi, answers = false, childMocks = false)
      }

      should("run successfully given valid params and members list is empty") {
        //given
        every { membersList.items } returns mutableListOf()
        every { listResponse.body() } returns membersList
        every { listCall.execute() } returns listResponse
        every { writeCall.execute() } returns writeResponse
        every { listResponse.isSuccessful } returns true
        every { writeResponse.isSuccessful } returns true

        //when
        classUnderTest.run(memberAllocationOperation, progressIndicator)

        //then
        verify(exactly = 1) {
          dataApi.writeToDatasetMember(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
          )
        }
      }

      should("run successfully given valid params") {
        //given
        every { member1.name } returns "AAAA"
        every { member2.name } returns "BBBB"
        every { membersList.items } returns mutableListOf(member1, member2)
        every { listResponse.body() } returns membersList
        every { listCall.execute() } returns listResponse
        every { writeCall.execute() } returns writeResponse
        every { listResponse.isSuccessful } returns true
        every { writeResponse.isSuccessful } returns true

        //when
        classUnderTest.run(memberAllocationOperation, progressIndicator)

        //then
        verify(exactly = 1) {
          dataApi.writeToDatasetMember(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
          )
        }
      }

      should("throw error if writeResponse was not successful") {
        //given
        every { writeResponse.isSuccessful } returns false
        every { writeResponse.code() } returns 403

        //when
        val exception = assertThrows<CallException> { classUnderTest.run(memberAllocationOperation, progressIndicator) }

        //then
        assertSoftly {
          exception shouldNotBe null
          exception.message shouldBe "Cannot create member TEST in ZOSMFAD.TEST on test_connection.\n" + "Code: 403"
        }
      }

      should("throw error if listResponse was not successful") {
        //given
        every { listResponse.isSuccessful } returns false
        every { listResponse.code() } returns 403

        //when
        val exception = assertThrows<CallException> { classUnderTest.run(memberAllocationOperation, progressIndicator) }

        //then
        assertSoftly {
          exception shouldNotBe null
          exception.message shouldBe "Cannot fetch member list for ZOSMFAD.TEST\n" + "Code: 403"
        }
      }

      should("throw error when listResponse was successful, but membersList contains duplicate member") {
        //given
        every { member1.name } returns "AAAA"
        every { member2.name } returns "TEST"
        every { membersList.items } returns mutableListOf(member1, member2)
        every { listResponse.body() } returns membersList
        every { listCall.execute() } returns listResponse
        every { listResponse.isSuccessful } returns true
        every { listResponse.code() } returns 404

        //when
        val exception = assertThrows<CallException> { classUnderTest.run(memberAllocationOperation, progressIndicator) }

        //then
        assertSoftly {
          exception shouldNotBe null
          exception.message shouldBe "Cannot create member TEST in ZOSMFAD.TEST on test_connection. Member with name TEST already exists.\n" + "Code: 404"
        }
      }
    }
  }
})
