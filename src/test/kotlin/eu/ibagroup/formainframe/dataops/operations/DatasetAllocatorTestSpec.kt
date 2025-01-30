/*
 * Copyright (c) 2024 IBA Group.
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
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.explorer.config.Presets
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestZosmfApiImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.zowe.kotlinsdk.CreateDataset
import org.zowe.kotlinsdk.DataAPI
import retrofit2.Call
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

class DatasetAllocatorTestSpec : WithApplicationShouldSpec({

  afterSpec {
    unmockkAll()
    clearAllMocks()
  }

  context("run dataset allocation operation") {
    val datasetAllocator = spyk<DatasetAllocator>()
    val progressIndicator = mockk<ProgressIndicator>()
    val connectionConfig = mockk<ConnectionConfig>()
    val datasetAllocationParams = mockk<DatasetAllocationParams>()
    val datasetAllocationOperation = mockk<DatasetAllocationOperation>()
    val createDataset = mockk<CreateDataset>()
    val dataApi = mockk<DataAPI>()
    val createDatasetCall = mockk<Call<Void>>()
    val createDatasetResponse = mockk<Response<Void>>()
    val writeMemberCall = mockk<Call<Void>>()
    val writeMemberResponse = mockk<Response<Void>>()
    val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl

    every { dataApi.createDataset(any(), any(), any()) } returns createDatasetCall
    every {
      dataApi.writeToDatasetMember(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
    } returns writeMemberCall

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

    beforeEach {
      every { progressIndicator.checkCanceled() } just Runs

      every { connectionConfig.name } returns "test_connection"
      every { connectionConfig.authToken } returns "auth_token"
      every { connectionConfig.uuid } returns "test_uuid"

      every { datasetAllocationParams.memberName } returns "test"
      every { datasetAllocationParams.datasetName } returns "ZOSMFAD.TEST"

      every { datasetAllocationParams.allocationParameters } returns createDataset
      every { datasetAllocationOperation.request } returns datasetAllocationParams
      every { datasetAllocationOperation.connectionConfig } returns connectionConfig
    }

    should("run successfully for presets without members") {
      listOf(
        Presets.CUSTOM_DATASET,
        Presets.SEQUENTIAL_DATASET,
        Presets.PDS_DATASET,
        Presets.PDSE_DATASET
      ).forEach { preset ->
        //given
        every { datasetAllocationParams.presets } returns preset
        every { createDatasetCall.execute() } returns createDatasetResponse
        every { createDatasetResponse.isSuccessful } returns true

        //when
        datasetAllocator.run(datasetAllocationOperation, progressIndicator)

        //then
        verify(exactly = 1) { dataApi.createDataset(any(), any(), any()) }
        clearMocks(dataApi, answers = false, childMocks = false)
      }
    }
    should("throw error if createDatasetResponse was not successful") {
      //given
      every { createDatasetCall.execute() } returns createDatasetResponse
      every { createDatasetResponse.isSuccessful } returns false
      every { createDatasetResponse.code() } returns 403

      //when
      val exception =
        assertThrows<CallException> { datasetAllocator.run(datasetAllocationOperation, progressIndicator) }

      //then
      assertSoftly {
        exception shouldNotBe null
        exception.message shouldBe "Cannot allocate dataset ZOSMFAD.TEST on test_connection\n" + "Code: 403"
      }
      clearMocks(dataApi, answers = false, childMocks = false)
    }

    should("run successfully for presets with members") {
      Presets.entries
        .filterNot {
          it == Presets.SEQUENTIAL_DATASET
            || it == Presets.PDS_DATASET
            || it == Presets.CUSTOM_DATASET
            || it == Presets.PDSE_DATASET
        }
        .forEach { preset ->
          //given
          every { datasetAllocationParams.presets } returns preset

          every { createDatasetCall.execute() } returns createDatasetResponse
          every { createDatasetResponse.isSuccessful } returns true
          every { writeMemberCall.execute() } returns writeMemberResponse
          every { writeMemberResponse.isSuccessful } returns true

          //when
          datasetAllocator.run(datasetAllocationOperation, progressIndicator)

          //then
          verify(exactly = 1) { dataApi.createDataset(any(), any(), any()) }
          verify(exactly = 1) {
            dataApi.writeToDatasetMember(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
          }
          clearMocks(dataApi, answers = false, childMocks = false)
        }
    }

    should("throw error if writeMemberResponse was not successful - allocation issue") {
      Presets.entries
        .filterNot {
          it == Presets.SEQUENTIAL_DATASET
            || it == Presets.PDS_DATASET
            || it == Presets.CUSTOM_DATASET
            || it == Presets.PDSE_DATASET
        }
        .forEach { preset ->
          //given
          every { datasetAllocationParams.presets } returns preset
          //given
          every { createDatasetCall.execute() } returns createDatasetResponse
          every { createDatasetResponse.isSuccessful } returns true
          every { writeMemberCall.execute() } throws RuntimeException("Unexpected error")

          //when
          verify { writeMemberCall.execute() }
          val exception =
            assertThrows<Exception> { datasetAllocator.run(datasetAllocationOperation, progressIndicator) }

          //then
          assertSoftly {
            exception shouldNotBe null
            exception.message shouldBe "Error allocating a new sample member test"
          }
          clearMocks(dataApi, answers = false, childMocks = false)
        }
    }

    should("throw error if writeMemberResponse was not successful - incorrect response") {
      Presets.entries
        .filterNot {
          it == Presets.SEQUENTIAL_DATASET
            || it == Presets.PDS_DATASET
            || it == Presets.CUSTOM_DATASET
            || it == Presets.PDSE_DATASET
        }
        .forEach { preset ->
          //given
          every { datasetAllocationParams.presets } returns preset
          //given
          every { createDatasetCall.execute() } returns createDatasetResponse
          every { createDatasetResponse.isSuccessful } returns true
          every { writeMemberCall.execute() } returns writeMemberResponse
          every { writeMemberResponse.isSuccessful } returns false
          every { writeMemberResponse.code() } returns 403

          //when
          verify { writeMemberCall.execute() }
          val exception =
            assertThrows<Throwable> { datasetAllocator.run(datasetAllocationOperation, progressIndicator) }

          //then
          assertSoftly {
            exception shouldNotBe null
            exception.cause?.message shouldBe "Cannot create sample member test in ZOSMFAD.TEST on test_connection\n" + "Code: 403"
          }
          clearMocks(dataApi, answers = false, childMocks = false)
        }
    }

    should("Progress Indicator Cancels Operation") {
      //given
      every { progressIndicator.checkCanceled() } throws CancellationException() // Simulate cancellation
      every { dataApi.createDataset(any(), any(), any()) } returns mockk()
      var isExceptionOccurred: Boolean = false
      //when
      try {
        datasetAllocator.run(datasetAllocationOperation, progressIndicator)
      } catch (e: CancellationException) {
        isExceptionOccurred = true
        // Expected behavior, no API calls should be made after cancellation
      }

      //then
      verify(atLeast = 1) { progressIndicator.checkCanceled() }
      verify { dataApi wasNot Called }
      assertSoftly {
        isExceptionOccurred shouldBe true
      }
    }
  }
})
