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

package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.dataops.fetch.JobFetchProvider
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestZosmfApiImpl
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.*
import org.zowe.kotlinsdk.ExecData
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.Job
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.InvocationTargetException

class JobFetchProviderTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("dataops module: fetch") {

    context("JobFetchProvider common") {
      val mockedConnectionConfig = mockk<ConnectionConfig>()
      every { mockedConnectionConfig.authToken } returns "auth_token"
      every { mockedConnectionConfig.url } returns "test_url"
      val mockedQuery = mockk<RemoteQuery<ConnectionConfig, JobsFilter, Unit>>()
      val mockedRequest = mockk<JobsFilter>()
      every { mockedRequest.owner } returns "TESTOWNER"
      every { mockedRequest.prefix } returns "TESTPREFIX"
      every { mockedRequest.userCorrelatorFilter } returns "TESTFILTER"
      val progressMockk = mockk<ProgressIndicator>()
      every { mockedQuery.request } returns mockedRequest
      every { mockedQuery.connectionConfig } returns mockedConnectionConfig

      val job1 = mockk<Job>()
      val job2 = mockk<Job>()
      every { job1.execStarted } returns null
      every { job1.execEnded } returns null
      every { job1.execSubmitted } returns null
      every { job2.execStarted } returns null
      every { job2.execEnded } returns null
      every { job2.execSubmitted } returns null
      every { job1.jobId } returns "TSUTEST1"
      every { job2.jobId } returns "TSUTEST2"
      every { job1.jobName } returns "TESTJOB1"
      every { job2.jobName } returns "TESTJOB2"

      val jobs = mutableListOf(job1, job2)
      val mockedCall = mockk<Call<List<Job>>>()
      val mockedResponse = mockk<Response<List<Job>>>()
      every { mockedCall.execute() } returns mockedResponse
      every { mockedResponse.body() } returns jobs

      val zosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
      zosmfApi.testInstance = mockk()
      val mockedApi = mockk<JESApi>()
      every { zosmfApi.testInstance.getApi(JESApi::class.java, mockedConnectionConfig) } returns mockedApi
      every {
        mockedApi.getFilteredJobs(
          basicCredentials = any() as String,
          owner = any() as String,
          prefix = any() as String,
          userCorrelator = any() as String,
          execData = any() as ExecData
        )
      } returns mockedCall
      every {
        mockedApi.getFilteredJobs(
          basicCredentials = any() as String,
          owner = any() as String,
          prefix = any() as String,
          userCorrelator = any() as String,
          execData = any() as ExecData
        ).cancelByIndicator(progressMockk)
      } returns mockedCall
      every {
        mockedApi.getFilteredJobs(
          basicCredentials = any() as String,
          owner = "*",
          prefix = "*",
          jobId = any() as String,
          execData = any() as ExecData
        )
      } returns mockedCall
      every {
        mockedApi.getFilteredJobs(
          basicCredentials = any() as String,
          owner = "*",
          prefix = "*",
          jobId = any() as String,
          execData = any() as ExecData
        ).cancelByIndicator(progressMockk)
      } returns mockedCall

      val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl

      // needed for cleanupUnusedFile test
      val mockedVirtualFile = mockk<MFVirtualFile>()
      val mockedFileAttributes = mockk<RemoteJobAttributes>()
      val mockedAttributesService = mockk<RemoteJobAttributesService>()
      val mockedJobsRequester = mockk<JobsRequester>()
      val requesters = mutableListOf(mockedJobsRequester)

      afterEach { unmockkAll() }

      val jobFetchProviderForTest = spyk(JobFetchProvider(dataOpsManagerService), recordPrivateCalls = true)

      should("fetchResponse get job attributes if job ID is not null and response is successful and exec dates/times are null") {

        val fetchResponseMethodRef =
          jobFetchProviderForTest::class.java.declaredMethods.first { it.name == "fetchResponse" }
        fetchResponseMethodRef.trySetAccessible()
        every { mockedRequest.jobId } returns "TSUTEST"
        every { mockedResponse.isSuccessful } returns true

        val jobAttributes =
          (fetchResponseMethodRef.invoke(jobFetchProviderForTest, mockedQuery, progressMockk) as Collection<*>)

        assertSoftly {
          jobAttributes shouldHaveSize jobs.size
        }
      }

      should("fetchResponse get job attributes if job ID is null and response is successful") {

        val fetchResponseMethodRef =
          jobFetchProviderForTest::class.java.declaredMethods.first { it.name == "fetchResponse" }
        fetchResponseMethodRef.trySetAccessible()
        every { mockedRequest.jobId } returns ""
        every { mockedResponse.isSuccessful } returns true

        val jobAttributes =
          (fetchResponseMethodRef.invoke(jobFetchProviderForTest, mockedQuery, progressMockk) as Collection<*>)

        assertSoftly {
          jobAttributes shouldHaveSize jobs.size
        }
      }

      should("fetchResponse get job attributes if exec dates/times are not null") {

        val fetchResponseMethodRef =
          jobFetchProviderForTest::class.java.declaredMethods.first { it.name == "fetchResponse" }
        fetchResponseMethodRef.trySetAccessible()
        every { mockedRequest.jobId } returns "TSUTEST"
        every { mockedResponse.isSuccessful } returns true

        every { job1.execStarted } returns ""
        every { job1.execEnded } returns ""
        every { job1.execSubmitted } returns ""
        every { job2.execStarted } returns ""
        every { job2.execEnded } returns ""
        every { job2.execSubmitted } returns ""

        val jobAttributes =
          (fetchResponseMethodRef.invoke(jobFetchProviderForTest, mockedQuery, progressMockk) as Collection<*>)

        assertSoftly {
          jobAttributes shouldHaveSize jobs.size
        }
      }

      should("fetchResponse get job attributes if response does not return any job") {

        val fetchResponseMethodRef =
          jobFetchProviderForTest::class.java.declaredMethods.first { it.name == "fetchResponse" }
        fetchResponseMethodRef.trySetAccessible()
        every { mockedRequest.jobId } returns "TSUTEST"
        every { mockedResponse.isSuccessful } returns true
        every { mockedResponse.body() } returns emptyList()

        val jobAttributes =
          (fetchResponseMethodRef.invoke(jobFetchProviderForTest, mockedQuery, progressMockk) as Collection<*>)

        assertSoftly {
          jobAttributes shouldHaveSize 0
        }
      }

      should("fetchResponse get job attributes if response was not successful") {
        val fetchResponseMethodRef =
          jobFetchProviderForTest::class.java.declaredMethods.first { it.name == "fetchResponse" }
        fetchResponseMethodRef.trySetAccessible()
        every { mockedRequest.jobId } returns "TSUTEST"
        every { mockedResponse.isSuccessful } returns false
        every { mockedResponse.code() } returns 404
        every { mockedResponse.body() } returns emptyList()
        every { mockedResponse.message() } returns "Unknown error"

        val exception = shouldThrow<InvocationTargetException> {
          fetchResponseMethodRef.invoke(jobFetchProviderForTest, mockedQuery, progressMockk)
        }

        assertSoftly {
          exception.cause!! shouldHaveMessage "Cannot retrieve Job files list\nCode: 404"
        }
      }

      should("cleanup unused file if connection config of the query is the same as for job file") {
        var cleanupPerformed = false
        val cleanupUnusedFileMethodRef =
          jobFetchProviderForTest::class.java.declaredMethods.first { it.name == "cleanupUnusedFile" }
        cleanupUnusedFileMethodRef.trySetAccessible()
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          @Suppress("UNCHECKED_CAST")
          override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
            attributesClass: Class<out A>,
            vFileClass: Class<out F>
          ): AttributesService<A, F> {
            return mockedAttributesService as AttributesService<A, F>
          }
        }

        every { mockedAttributesService.getAttributes(mockedVirtualFile) } returns mockedFileAttributes
        every { mockedAttributesService.clearAttributes(mockedVirtualFile) } just Runs

        every { mockedJobsRequester.connectionConfig } returns mockedConnectionConfig
        every { mockedFileAttributes.requesters } returns requesters

        every { mockedVirtualFile.delete(any() as JobFetchProvider) } answers {
          cleanupPerformed = true
        }

        cleanupUnusedFileMethodRef.invoke(jobFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          cleanupPerformed shouldBe true
        }
      }

      should("cleanup file if connections are not the same") {
        var cleanupPerformed = false
        val cleanupUnusedFileMethodRef =
          jobFetchProviderForTest::class.java.declaredMethods.first { it.name == "cleanupUnusedFile" }
        cleanupUnusedFileMethodRef.trySetAccessible()

        every { mockedJobsRequester.connectionConfig } returns mockk()
        every { mockedFileAttributes.requesters } returns requesters
        every {
          mockedAttributesService.updateAttributes(
            mockedVirtualFile,
            any() as RemoteJobAttributes.() -> Unit
          )
        } answers {
          cleanupPerformed = true
          secondArg<RemoteJobAttributes.() -> Unit>().invoke(mockedFileAttributes)
        }

        cleanupUnusedFileMethodRef.invoke(jobFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          cleanupPerformed shouldBe true
        }
      }

    }
  }
})