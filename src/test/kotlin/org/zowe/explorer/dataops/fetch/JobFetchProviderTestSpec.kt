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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.*
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.ExecData
import org.zowe.kotlinsdk.JESApi
import org.zowe.kotlinsdk.Job
import retrofit2.Call
import retrofit2.Response
import java.lang.reflect.InvocationTargetException

class JobFetchProviderTestSpec : AppInitShouldSpec("dataops/JobFetchProvider", {
  context("JobFetchProvider common") {
    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val mockedConnectionConfig = mockk<ConnectionConfig> {
      every { uuid } returns "test"
      every { url } returns "test_url"
    }
    val mockedRequest = mockk<JobsFilter> {
      every { owner } returns "TESTOWNER"
      every { prefix } returns "TESTPREFIX"
      every { userCorrelatorFilter } returns "TESTFILTER"
    }
    val mockedQuery = mockk<RemoteQuery<ConnectionConfig, JobsFilter, Unit>> {
      every { request } returns mockedRequest
      every { connectionConfig } returns mockedConnectionConfig
    }
    val progressMockk = mockk<ProgressIndicator>()
    val job1 = mockk<Job> {
      every { execStarted } returns null
      every { execEnded } returns null
      every { execSubmitted } returns null
      every { jobId } returns "TSUTEST1"
      every { jobName } returns "TESTJOB1"
    }
    val job2 = mockk<Job> {
      every { execStarted } returns null
      every { execEnded } returns null
      every { execSubmitted } returns null
      every { jobId } returns "TSUTEST2"
      every { jobName } returns "TESTJOB2"
    }
    val jobs = mutableListOf(job1, job2)
    val mockedResponse = mockk<Response<List<Job>>> {
      every { body() } returns jobs
    }
    val mockedCall = mockk<Call<List<Job>>> {
      every { execute() } returns mockedResponse
    }

    val zosmfApi = ZosmfApi.getService()
    every { zosmfApi.getApi(JESApi::class.java, mockedConnectionConfig) } returns mockk<JESApi> {
      every {
        getFilteredJobs(
          basicCredentials = any() as String,
          owner = any() as String,
          prefix = any() as String,
          userCorrelator = any() as String,
          execData = any() as ExecData
        )
      } returns mockedCall
      every {
        getFilteredJobs(
          basicCredentials = any() as String,
          owner = any() as String,
          prefix = any() as String,
          userCorrelator = any() as String,
          execData = any() as ExecData
        ).cancelByIndicator(progressMockk)
      } returns mockedCall
      every {
        getFilteredJobs(
          basicCredentials = any() as String,
          owner = "*",
          prefix = "*",
          jobId = any() as String,
          execData = any() as ExecData
        )
      } returns mockedCall
      every {
        getFilteredJobs(
          basicCredentials = any() as String,
          owner = "*",
          prefix = "*",
          jobId = any() as String,
          execData = any() as ExecData
        ).cancelByIndicator(progressMockk)
      } returns mockedCall
    }

    val dataOpsManagerService = DataOpsManager.getService()

    // needed for cleanupUnusedFile test
    val mockedVirtualFile = mockk<MFVirtualFile>()
    val mockedFileAttributes = mockk<RemoteJobAttributes>()
    val mockedAttributesService = mockk<RemoteJobAttributesService>()
    val mockedJobsRequester = mockk<JobsRequester>()
    val requesters = mutableListOf(mockedJobsRequester)

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
      every {
        dataOpsManagerService.getAttributesService(RemoteJobAttributes::class.java, MFVirtualFile::class.java)
      } returns mockedAttributesService

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
})
