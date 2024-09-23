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

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.attributes.JobsRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.fetch.JobFetchHelper
import eu.ibagroup.formainframe.dataops.log.JobLogFetcher
import eu.ibagroup.formainframe.dataops.log.JobProcessInfo
import eu.ibagroup.formainframe.utils.asMutableList
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.kotlinsdk.Job

class JobFetchHelperTestSpec : ShouldSpec({
  context("dataops module: fetch") {

    context("JobFetchHelper common") {

      val mockedConnectionConfig = mockk<ConnectionConfig>()

      val mockedJobFilter = mockk<JobsFilter>()
      every { mockedJobFilter.jobId } returns "TEST"
      every { mockedJobFilter.owner } returns "ZOSMFAD"
      every { mockedJobFilter.prefix } returns "TEST*"

      val jobQuery = mockk<RemoteQuery<ConnectionConfig, JobsFilter, Unit>>()
      every { jobQuery.connectionConfig } returns mockedConnectionConfig
      every { jobQuery.request } returns mockedJobFilter

      val jobInfoForTest = Job("TSU06062", "TESTJOB", "TEST1",
        "ZOSMFAD", Job.Status.OUTPUT, Job.JobType.JOB, null, "CC=00",
        "test_url", "test_url", null, 1, "phase",
        emptyList(), null, null, null, null, null, null
      )
      val mockedJobAttributesForTest = mockk<RemoteJobAttributes>()
      every { mockedJobAttributesForTest.jobInfo } returns jobInfoForTest
      every { mockedJobAttributesForTest.url } returns jobInfoForTest.url
      every { mockedConnectionConfig.url } returns jobInfoForTest.url
      every { mockedJobAttributesForTest.requesters } returns JobsRequester(jobQuery.connectionConfig, jobQuery.request).asMutableList()

      var updatedJobAttributes: RemoteJobAttributes?

      beforeEach { updatedJobAttributes = null }
      afterEach { unmockkAll() }

      context("JobFetchHelper create instance and perform tests") {

        should("run job fetch helper if fetch spool file returns empty logs") {

          val jobFetchHelperForTest = spyk(JobFetchHelper(jobQuery, mockedJobAttributesForTest), recordPrivateCalls = true)
          val jobFetcherField = jobFetchHelperForTest::class.java.getDeclaredField("jobLogFetcher")
          jobFetcherField.isAccessible = true
          val jobFetcher = jobFetcherField.get(jobFetchHelperForTest) as JobLogFetcher
          mockkObject(jobFetcher)

          val emptySpoolContent = emptyArray<String>()
          every { jobFetcher.fetchLogsBySpoolId(any() as JobProcessInfo, any() as Int) } returns emptySpoolContent

          jobFetchHelperForTest.start()
          while(true) {
            if (!jobFetchHelperForTest.isAlive) {
              updatedJobAttributes = jobFetchHelperForTest.getUpdatedJobAttributes()
              break
            }
          }
          assertSoftly {
            updatedJobAttributes?.jobInfo?.execStarted?.trim() shouldBe ""
            updatedJobAttributes?.jobInfo?.execEnded?.trim() shouldBe "JCL NOT AVAILABLE"
          }
        }

        should("run job fetch helper if fetch spool file returns started/ended date and time") {

          val jobFetchHelperForTest = spyk(JobFetchHelper(jobQuery, mockedJobAttributesForTest), recordPrivateCalls = true)
          val jobFetcherField = jobFetchHelperForTest::class.java.getDeclaredField("jobLogFetcher")
          jobFetcherField.isAccessible = true
          val jobFetcher = jobFetcherField.get(jobFetchHelperForTest) as JobLogFetcher
          mockkObject(jobFetcher)

          val spoolContent = arrayOf(
            "J E S 2  J O B  L O G  --  S Y S T E M  S 0 W 1  --  N O D E  S 0 W 1\n" +
                "0\n" +
                "19.45.23 TSU06062 ---- TUESDAY,   20 JUN 2023 ----\n" +
                "19.45.23 TSU06062  HASP373 ZOSMFAD  STARTED\n" +
                "19.45.23 TSU06062  IEF125I ZOSMFAD - LOGGED ON - TIME=19.45.23\n" +
                "20.09.28 TSU06062  BPXP018I THREAD 2037180000000000, IN PROCESS 83952040, ENDED  856\n" +
                "856             WITHOUT BEING UNDUBBED WITH COMPLETION CODE 40222000\n" +
                "856             , AND REASON CODE 00000000.\n" +
                "20.09.29 TSU06062  IEF450I ZOSMFAD IZUFPROC IZUFPROC - ABEND=S222 U0000 REASON=00000000  858\n" +
                "858                     TIME=20.09.29\n" +
                "20.09.29 TSU06062  HASP395 TESTJOB  ENDED - ABEND=S222\n" +
                "0------ JES2 JOB STATISTICS ------\n" +
                "-  20 JUN 2023 JOB EXECUTION DATE\n" +
                "    -            3 CARDS READ\n" +
                "    -          148 SYSOUT PRINT RECORDS\n" +
                "    -            0 SYSOUT PUNCH RECORDS\n" +
                "    -           13 SYSOUT SPOOL KBYTES\n" +
                "    -        24.09 MINUTES EXECUTION TIME"
          )
          every { jobFetcher.fetchLogsBySpoolId(any() as JobProcessInfo, any() as Int) } returns spoolContent

          jobFetchHelperForTest.start()
          while(true) {
            if (!jobFetchHelperForTest.isAlive) {
              updatedJobAttributes = jobFetchHelperForTest.getUpdatedJobAttributes()
              break
            }
          }
          assertSoftly {
            updatedJobAttributes?.jobInfo?.execStarted?.trim() shouldBe "20 JUN 2023 19.45.23"
            updatedJobAttributes?.jobInfo?.execEnded?.trim() shouldBe "20 JUN 2023 20.09.29"
          }
        }

        should("run job fetch helper if fetch spool file returns started/ended date and time if ended date was not found") {

          val jobFetchHelperForTest = spyk(JobFetchHelper(jobQuery, mockedJobAttributesForTest), recordPrivateCalls = true)
          val jobFetcherField = jobFetchHelperForTest::class.java.getDeclaredField("jobLogFetcher")
          jobFetcherField.isAccessible = true
          val jobFetcher = jobFetcherField.get(jobFetchHelperForTest) as JobLogFetcher
          mockkObject(jobFetcher)

          val spoolContent = arrayOf(
            "J E S 2  J O B  L O G  --  S Y S T E M  S 0 W 1  --  N O D E  S 0 W 1\n" +
                "0\n" +
                "19.45.23 TSU06061 ---- TUESDAY,   20 JUN 2023 ----\n" +
                "19.45.23 TSU06062  HASP373 TESTJOB  STARTED\n" +
                "19.45.23 TSU06062  IEF125I ZOSMFAD - LOGGED ON - TIME=19.45.23\n" +
                "20.09.28 TSU06062  BPXP018I THREAD 2037180000000000, IN PROCESS 83952040, ENDED  856\n" +
                "856             WITHOUT BEING UNDUBBED WITH COMPLETION CODE 40222000\n" +
                "856             , AND REASON CODE 00000000.\n" +
                "20.09.29 TSU06062  IEF450I ZOSMFAD IZUFPROC IZUFPROC - ABEND=S222 U0000 REASON=00000000  858\n" +
                "858                     TIME=20.09.29\n" +
                "20.09.29 TSU06062  HASP395 TESTJOB  ENDED - ABEND=S222\n" +
                "0------ JES2 JOB STATISTICS ------\n" +
                "-  20 JUN 2023 JOB\n" +
                "    -            3 CARDS READ\n" +
                "    -          148 SYSOUT PRINT RECORDS\n" +
                "    -            0 SYSOUT PUNCH RECORDS\n" +
                "    -           13 SYSOUT SPOOL KBYTES\n" +
                "    -        24.09 MINUTES EXECUTION TIME"
          )
          every { jobFetcher.fetchLogsBySpoolId(any() as JobProcessInfo, any() as Int) } returns spoolContent

          jobFetchHelperForTest.start()
          while(true) {
            if (!jobFetchHelperForTest.isAlive) {
              updatedJobAttributes = jobFetchHelperForTest.getUpdatedJobAttributes()
              break
            }
          }
          assertSoftly {
            updatedJobAttributes?.jobInfo?.execStarted?.trim() shouldBe "20 JUN 2023 19.45.23"
            updatedJobAttributes?.jobInfo?.execEnded?.trim() shouldBe "20 JUN 2023 20.09.29"
          }
        }

        should("run job fetch helper if fetch spool file returns no date and time in it") {

          val jobFetchHelperForTest = spyk(JobFetchHelper(jobQuery, mockedJobAttributesForTest), recordPrivateCalls = true)
          val jobFetcherField = jobFetchHelperForTest::class.java.getDeclaredField("jobLogFetcher")
          jobFetcherField.isAccessible = true
          val jobFetcher = jobFetcherField.get(jobFetchHelperForTest) as JobLogFetcher
          mockkObject(jobFetcher)

          val spoolContent = arrayOf("J E S 2  J O B  L O G  --  S Y S T E M  S 0 W 1  --  N O D E  S 0 W 1")
          every { jobFetcher.fetchLogsBySpoolId(any() as JobProcessInfo, any() as Int) } returns spoolContent

          jobFetchHelperForTest.start()
          while(true) {
            if (!jobFetchHelperForTest.isAlive) {
              updatedJobAttributes = jobFetchHelperForTest.getUpdatedJobAttributes()
              break
            }
          }
          assertSoftly {
            updatedJobAttributes?.jobInfo?.execStarted?.trim() shouldBe ""
            updatedJobAttributes?.jobInfo?.execEnded?.trim() shouldBe ""
          }
        }

        should("run job fetch helper if fetch spool file throws an exception") {

          val exception: Throwable?
          val jobFetchHelperForTest = spyk(JobFetchHelper(jobQuery, mockedJobAttributesForTest), recordPrivateCalls = true)
          val jobFetcherField = jobFetchHelperForTest::class.java.getDeclaredField("jobLogFetcher")
          jobFetcherField.isAccessible = true
          val jobFetcher = jobFetcherField.get(jobFetchHelperForTest) as JobLogFetcher
          mockkObject(jobFetcher)

          every { jobFetcher.fetchLogsBySpoolId(any() as JobProcessInfo, any() as Int) } answers {
            throw IllegalArgumentException("TEST FAILED")
          }

          jobFetchHelperForTest.start()
          while(true) {
            if (!jobFetchHelperForTest.isAlive) {
              exception = jobFetchHelperForTest.getException()
              break
            }
          }
          assertSoftly {
            exception shouldNotBe null
          }
        }
      }
    }
  }
})
