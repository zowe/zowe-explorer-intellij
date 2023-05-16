/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package jes

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.keyboard
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.event.KeyEvent
import java.io.File

/**
 * Tests cancelling, holding, releasing jobs and checks results.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class CancelHoldReleaseJobTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog", "Add JES Working Set Dialog", "Add Working Set Dialog"
    )
    private val projectName = "untitled"
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val datasetName = "$ZOS_USERID.CANCEL.JOBS".uppercase()
    private val jobName = "TEST1"
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/"
    private val fileName = "job_rc0.txt"

    private enum class JobAction { SUBMIT, CANCEL, HOLD, RELEASE }

    private var mapListDatasets = mutableMapOf<String, String>()
    private var listMembersInDataset = mutableListOf<String>()
    private var isFirst = true

    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        startMockServer()
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(
            testInfo,
            connectionName,
            projectName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot
        )
        createWS(remoteRobot)
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${datasetName}_restfiles_post",
            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${datasetName}") ?: false },
            { MockResponse().setBody("{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
        )
        mapListDatasets[datasetName] = listDS(datasetName)
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles",
            {
                it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetName}")
                    ?: false
            },
            { MockResponse().setBody(buildFinalListDatasetJson()) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_listmembers",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false },
            {
                MockResponse().setBody(buildListMembersJson())
            }
        )
        allocatePDSAndCreateMask(wsName, datasetName, projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createJob(testInfo, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles",
            {
                it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetName.uppercase()}")
                    ?: false
            },
            { MockResponse().setBody("{}") }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_delete",
            { it?.requestLine?.contains("DELETE /zosmf/restfiles/ds/${datasetName.uppercase()}") ?: false },
            { MockResponse().setBody("{}") }
        )
        deleteDataset(datasetName, projectName, fixtureStack, remoteRobot)
        mockServer.shutdown()
        clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            close()
        }
    }

    /**
     * Closes all unclosed closable fixtures that we want to close.
     */
    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) = with(remoteRobot) {
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
        closeNotificationsAndJobsTabsIfExist(remoteRobot)
        responseDispatcher.removeAllEndpoints()
    }

    /**
     * Test to cancel one job and check that correct info is returned.
     */
    @Test
    @Order(1)
    fun testCancelOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.ACTIVE, remoteRobot)
        val jobId = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.CANCEL, remoteRobot)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to release one job and check that correct info is returned.
     */
    @Test
    @Order(2)
    fun testHoldReleaseOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.INPUT, remoteRobot)
        val jobId = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.HOLD, remoteRobot)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.RELEASE, remoteRobot)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId, remoteRobot)
        Thread.sleep(2000)
        checkJobResultInConsole(JobAction.SUBMIT, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to cancel one job and check that correct info is returned.
     */
    @Test
    @Order(3)
    fun testHoldCancelOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.INPUT, remoteRobot)
        val jobId = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.HOLD, remoteRobot)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.CANCEL, remoteRobot)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to release, then to cancel one job and check that correct info is returned.
     */
    @Test
    @Order(4)
    fun testHoldReleaseCancelOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.INPUT, remoteRobot)
        val jobId = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.HOLD, remoteRobot)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.RELEASE, remoteRobot, JobStatus.INPUT)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.CANCEL, remoteRobot)
        Thread.sleep(2000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Closes notifications and jobs tabs in jobs panel if exists.
     */
    private fun closeNotificationsAndJobsTabsIfExist(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            try {
                find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']"))
                    .click()
                find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
            } catch (ex: Exception) {
                when (ex) {
                    !is WaitForConditionTimeoutException -> throw ex
                }
            }

            try {
                findAll<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).forEach {
                    it.click()
                    keyboard {
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
                    }
                }
            } catch (ex: Exception) {
                when (ex) {
                    !is WaitForConditionTimeoutException -> throw ex
                }
            }
        }
    }

    /**
     * Gets jobId for submitted job.
     */
    private fun getIdSubmittedJob(remoteRobot: RemoteRobot): String = with(remoteRobot) {
        var jobId = ""
        ideFrameImpl(projectName, fixtureStack) {
            jobId = find<ContainerFixture>(byXpath("//div[@class='Tree']")).findAllText()[2].text.trim()
        }
        return jobId
    }

    /**
     * Checks jobs result.
     */
    private fun checkJobResultInConsole(action: JobAction, jobId: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val rc = when (action) {
                JobAction.CANCEL -> "CANCELED"
                JobAction.SUBMIT -> "CC 0000"
                else -> throw Exception("Unknown action")
            }
            ideFrameImpl(projectName, fixtureStack) {
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "JOB $jobName($jobId) EXECUTED"
                )
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "OWNER: ${ZOS_USERID.uppercase()}"
                )
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "RETURN CODE: $rc"
                )
            }
        }

    /**
     * Closes tab in jobs panel.
     */
    private fun closeJobTabInJobsPanel(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            findAll<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).last()
                .findText(
                    "//'$datasetName($jobName)'"
                ).click()
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
            }
        }
    }

    /**
     * Performs action (cancel/hold/release) on running job.
     */
    private fun doActionOnRunningJob(
        testInfo: TestInfo,
        action: JobAction,
        remoteRobot: RemoteRobot,
        jobStatus: JobStatus = JobStatus.OUTPUT
    ) =
        with(remoteRobot) {
            responseDispatcher.removeAllEndpoints()
            val myAction: String
            when (action) {
                JobAction.CANCEL -> {
                    myAction = "Cancel Job ()"
                    responseDispatcher.injectEndpoint(
                        "${testInfo.displayName}_cancel",
                        { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs/$jobName/JOB07380 ") ?: false },
                        {
                            MockResponse().setBody("{\"request\":\"CANCEL\",\"version\":\"2.0\"}")
                                .setBody(setBodyJobCancelled(jobName))
                        }
                    )
                    responseDispatcher.injectEndpoint(
                        "${testInfo.displayName}_cancel_files",
                        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files HTTP") ?: false },
                        {
                            MockResponse().setBody(replaceInJson("getSpoolFiles", jobName, "CANCELED"))
                        }
                    )
                    responseDispatcher.injectEndpoint(
                        "${testInfo.displayName}_cancel_files2",
                        {
                            it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files/2/records")
                                ?: false
                        },
                        {
                            MockResponse().setBody(
                                File(filePath + "mock/getSpoolFileContentCanceled.txt").readText(
                                    Charsets.UTF_8
                                )
                            )
                        }
                    )
                    responseDispatcher.injectEndpoint(
                        "${testInfo.displayName}_cancel_files3",
                        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: false },
                        { MockResponse().setBody(setBodyJobCancelled(jobName)) }
                    )
                }

                JobAction.HOLD -> {
                    myAction = "Hold Job ()"
                    responseDispatcher.injectEndpoint(
                        "${testInfo.displayName}_hold",
                        { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs/$jobName/JOB07380 ") ?: false },
                        {
                            MockResponse().setBody("{\"request\":\"HOLD\",\"version\":\"2.0\"}")
                                .setBody(setBodyJobHeldOrReleased(jobName))
                        }
                    )
                    responseDispatcher.injectEndpoint(
                        "${testInfo.displayName}_hold_files3",
                        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: false },
                        { MockResponse().setBody(setBodyJobSubmit(jobName, JobStatus.INPUT)) }
                    )
                }

                JobAction.RELEASE -> {
                    myAction = "Release Job ()"
                    responseDispatcher.injectEndpoint(
                        "${testInfo.displayName}_release",
                        { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs/$jobName/JOB07380 ") ?: false },
                        {
                            MockResponse().setBody("{\"request\":\"RELEASE\",\"version\":\"2.0\"}")
                                .setBody(setBodyJobHeldOrReleased(jobName))
                        }
                    )
                    if (jobStatus == JobStatus.OUTPUT) {
                        responseDispatcher.injectEndpoint(
                            "${testInfo.displayName}_release_files",
                            {
                                it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files HTTP")
                                    ?: false
                            },
                            { MockResponse().setBody(replaceInJson("getSpoolFiles", jobName, "CC 0000")) }
                        )
                        responseDispatcher.injectEndpoint(
                            "${testInfo.displayName}_release_files2",
                            {
                                it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files/2/records")
                                    ?: false
                            },
                            {
                                MockResponse().setBody(
                                    File(filePath + "mock/getSpoolFileContentRC00.txt").readText(
                                        Charsets.UTF_8
                                    )
                                )
                            }
                        )
                        responseDispatcher.injectEndpoint(
                            "${testInfo.displayName}_release_files3",
                            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: false },
                            { MockResponse().setBody(replaceInJson("getStatus", jobName, "CC 0000", JobStatus.OUTPUT)) }
                        )
                    } else {
                        responseDispatcher.injectEndpoint(
                            "${testInfo.displayName}_release_files3",
                            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: false },
                            { MockResponse().setBody(setBodyJobSubmit(jobName, JobStatus.INPUT)) }
                        )
                    }
                }

                else -> throw Exception("Unknown action")
            }

            ideFrameImpl(projectName, fixtureStack) {
                clickActionButton(byXpath("//div[@class='ActionButton' and @myaction='$myAction']"))
            }
        }

    /**
     * Checks and closes notification after performed action on running job.
     */
    private fun checkNotificationAfterJobAction(
        action: JobAction,
        jobId: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val textToFind = when (action) {
            JobAction.CANCEL -> "$jobName: $jobId has been cancelled"
            JobAction.HOLD -> "$jobName: $jobId has been held"
            JobAction.RELEASE -> "$jobName: $jobId has been released"
            JobAction.SUBMIT -> "Job $jobName has been submitted"
        }
        ideFrameImpl(projectName, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText(textToFind)
                .click()
            find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
        }
    }

    /**
     * Creates empty working set.
     */
    private fun createWS(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(500)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(500)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    /**
     * Creates job in dataset.
     */
    private fun createJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        openLocalFileAndCopyContent(filePath + fileName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(3000)
        createMemberAndPasteContentWithMock(testInfo, datasetName, jobName, fileName, remoteRobot)
    }

    /**
     * Creates member in dataset and pastes content from buffer.
     */
    private fun createMemberAndPasteContentWithMock(
        testInfo: TestInfo, datasetName: String,
        memberName: String, fileName: String, remoteRobot: RemoteRobot
    ) {
        var isFirstRequest = true
        listMembersInDataset.add(memberName)
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${memberName}in${datasetName}_restfiles",
            { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberName})") ?: false && isFirstRequest && isFirst },
            { MockResponse().setBody("") }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_fill_${memberName}_restfiles",
            { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberName})") ?: false && !isFirstRequest && isFirst },
            { MockResponse().setBody(File(filePath + fileName).readText(Charsets.UTF_8)) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_listmembers1",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false },
            { MockResponse().setBody(buildListMembersJson()) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_getmember1",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}($memberName)") ?: false && isFirst },
            { MockResponse().setBody("") }
        )

        createEmptyDatasetMember(
            datasetName, memberName, projectName, fixtureStack, remoteRobot
        )
        isFirstRequest = false
        pasteContent(memberName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(3000)
        isFirst = false
    }

    /**
     * Creates json for dataset list to display it in File Explorer.
     */
    private fun buildFinalListDatasetJson(): String {
        var result = "{}"
        if (mapListDatasets.isNotEmpty()) {
            var listDatasetsJson = "{\"items\":["
            mapListDatasets.forEach {
                listDatasetsJson += it.value
            }
            result = listDatasetsJson.dropLast(1) + "],\n" +
                "  \"returnedRows\": ${mapListDatasets.size},\n" +
                "  \"totalRows\": ${mapListDatasets.size},\n" +
                "  \"JSONversion\": 1\n" +
                "}"
        }
        return result
    }

    /**
     * Creates json for dataset members to display it in File Explorer.
     */
    private fun buildListMembersJson(): String {
        var members = "[ "
        if (listMembersInDataset.isNotEmpty()) {
            listMembersInDataset.forEach { members += "{\"member\": \"${it}\"}," }
        }
        members = members.dropLast(1) + "]"
        return "{\"items\":$members,\"returnedRows\": ${listMembersInDataset.size},\"JSONversion\": 1}"
    }


    /**
     * Submits job on mock server.
     */
    private fun submitJobWithMock(
        testInfo: TestInfo,
        datasetName: String,
        jobName: String,
        jobStatus: JobStatus,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val spoolFileContent = "mock/getSpoolFileContentRC00.txt"
        val rc = "CC 0000"
        val statusJson: String = when (jobStatus) {
            JobStatus.ACTIVE -> "getActiveStatus"
            else -> "getStatus"
        }
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs",
            { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs") ?: false },
            {
                MockResponse().setBody("{\"file\":\"//'$datasetName($jobName)'\"}")
                    .setBody(setBodyJobSubmit(jobName, jobStatus))
            }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs_files",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files HTTP") ?: false },
            { MockResponse().setBody(replaceInJson("getSpoolFiles", jobName, rc)) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs_files2",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files/2/records") ?: false },
            { MockResponse().setBody(File(filePath + spoolFileContent).readText(Charsets.UTF_8)) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs_files3",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: false },
            { MockResponse().setBody(replaceInJson(statusJson, jobName, rc, jobStatus)) }
        )
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        responseDispatcher.removeAllEndpoints()
    }

    private fun setBodyJobCancelled(jobName: String): String {
        return "{\n" +
            "\"jobid\":\"JOB07380\",\n" +
            "\"jobname\":\"$jobName\",\n" +
            "\"original-jobid\":\"JOB07380\",\n" +
            "\"owner\":\"${ZOS_USERID.uppercase()}\",\n" +
            "\"member\":\"JES2\",\n" +
            "\"sysname\":\"SY1\",\n" +
            "\"job-correlator\":\"JOB07380SY1.....CC20F378.......:\",\n" +
            "\"status\":\"0\",\n" +
            "\"retcode\":\"CANCELED\"\n" +
            "}"
    }

    private fun setBodyJobHeldOrReleased(jobName: String): String {
        return "{\n" +
            "\"jobid\":\"JOB07380\",\n" +
            "\"jobname\":\"$jobName\",\n" +
            "\"original-jobid\":\"JOB07380\",\n" +
            "\"owner\":\"${ZOS_USERID.uppercase()}\",\n" +
            "\"member\":\"JES2\",\n" +
            "\"sysname\":\"SY1\",\n" +
            "\"job-correlator\":\"JOB07380SY1.....CC20F378.......:\",\n" +
            "\"status\":\"0\"\n" +
            "}"
    }

    private fun replaceInJson(
        fileName: String,
        jobName: String,
        rc: String,
        jobStatus: JobStatus = JobStatus.OUTPUT
    ): String {
        return (responseDispatcher.readMockJson(fileName) ?: "").replace("hostName", mockServer.hostName)
            .replace("port", mockServer.port.toString())
            .replace("jobName", jobName)
            .replace("retCode", rc).replace("jobStatus", jobStatus.name)
    }
}