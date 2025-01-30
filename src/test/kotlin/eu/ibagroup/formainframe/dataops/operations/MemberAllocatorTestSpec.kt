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

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestCredentialsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestZosmfApiImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.MembersList
import retrofit2.Call
import retrofit2.Response

class MemberAllocatorTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("MemberAllocator test spec") {

    val classUnderTest = spyk<MemberAllocator>()

    context("run operation") {

      val progressIndicator = mockk<ProgressIndicator>()
      every { progressIndicator.checkCanceled() } just Runs

      val credentialService = CredentialService.getService() as TestCredentialsServiceImpl
      credentialService.testInstance = object : TestCredentialsServiceImpl() {
        override fun getUsernameByKey(connectionConfigUuid: String): String {
          return "test"
        }

        override fun getPasswordByKey(connectionConfigUuid: String): CharArray? {
          return "test".toCharArray()
        }
      }

      val connectionConfig = mockk<ConnectionConfig>()
      every { connectionConfig.name } returns "test_connection"
      every { connectionConfig.uuid } returns "test_uuid"
      val memberAllocationParams = mockk<MemberAllocationParams>()
      val memberAllocationOperation = mockk<MemberAllocationOperation>()
      every { memberAllocationParams.memberName } returns "test"
      every { memberAllocationParams.datasetName } returns "ZOSMFAD.TEST"
      every { memberAllocationOperation.request } returns memberAllocationParams
      every { memberAllocationOperation.connectionConfig } returns connectionConfig

      val dataApi = mockk<DataAPI>()
      val listCall = mockk<Call<MembersList>>()
      val listResponse = mockk<Response<MembersList>>()
      val writeCall = mockk<Call<Void>>()
      val writeResponse = mockk<Response<Void>>()
      val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
      zosmfApi.testInstance = object : TestZosmfApiImpl() {
        override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
          return if (apiClass == DataAPI::class.java) {
            dataApi as Api
          } else {
            super.getApi(apiClass, connectionConfig)
          }
        }

        override fun <Api : Any> getApiWithBytesConverter(
          apiClass: Class<out Api>,
          connectionConfig: ConnectionConfig
        ): Api {
          return if (apiClass == DataAPI::class.java) {
            dataApi as Api
          } else {
            super.getApiWithBytesConverter(apiClass, connectionConfig)
          }
        }
      }
      every { dataApi.listDatasetMembers(any(), any(), any(), any(), any(), any(), any()) } returns listCall
      every {
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
      } returns writeCall

      val membersList = mockk<MembersList>()
      val member1 = mockk<Member>()
      val member2 = mockk<Member>()

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
        clearMocks(dataApi, answers = false, childMocks = false)
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
        clearMocks(dataApi, answers = false, childMocks = false)
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
      unmockkAll()
    }
  }
})
