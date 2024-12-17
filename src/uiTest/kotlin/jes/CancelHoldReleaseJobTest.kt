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
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import testutils.injectSingleMember
import workingset.*
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.*
import workingset.*
import java.awt.event.KeyEvent
import java.time.Duration

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

    private val connectionName = "valid connection"
    private val datasetBody = "{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}"
    private val wsName = "WS1"
    private val datasetName = "$ZOS_USERID.CANCEL.JOBS".uppercase()
    private val datasetMaskPair = Pair(datasetName, ZOS_MASK)
    private val jobName = "TEST1"
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/"
    private val fileName = "job_rc0.txt"
    private val spoolFileName = "getSpoolFiles"
    private val statusFileName = "getStatus"
    private val dirFileNameForCancelSpool = "mock/getSpoolFileContentCanceled.txt"
    private val dirFileNameForFileContent = "mock/getSpoolFileContentRC00.txt"

    private enum class JobAction { SUBMIT, CANCEL, HOLD, RELEASE}

    private var mapListDatasets = mutableMapOf<String, String>()
    private var listMembersInDataset = mutableListOf<String>()
    private var utilObject = IdeaInteractionClass()
    private lateinit var processManager: ProcessManager


    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(testInfo, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        injectTestInfoForPdsDataset(testInfo.displayName, datasetBody, datasetName)
        mapListDatasets[datasetName] = listDS(datasetName, PDS_TYPE, PO_ORG_SHORT)
        injectListAllAllocatedDatasetsWithContents(testInfo,datasetName,mapListDatasets)
        injectSingleMember(testInfo, datasetName, listMembersInDataset)
        utilObject.createWsAndMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot,datasetMaskPair)
        openOrCloseWorkingSetInExplorer(wsName, fixtureStack, remoteRobot)
        createMemberAndPasteContentWithMock(testInfo, datasetName, jobName, fileName, remoteRobot)

    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(){
        processManager.close()
        mockServer.shutdown()
    }

    /**
     * Closes all unclosed closable fixtures that we want to close.
     */
    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot){
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
        closeNotificationsAndJobsTabsIfExist(remoteRobot)
        responseDispatcher.removeAllEndpoints()
    }

    /**
     * Test to cancel one job and check that correct info is returned.
     */

    @Test
    fun testCancelOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.ACTIVE, remoteRobot)
        Thread.sleep(2000)
        val jobId = find<ContainerFixture>(jobSubmittedIdLoc).findAllText()[2].text.trim()
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.CANCEL, remoteRobot)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to release one job and check that correct info is returned.
     */
    @Test
    fun testHoldReleaseOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.INPUT, remoteRobot)
        val jobId = find<ContainerFixture>(jobSubmittedIdLoc).findAllText()[2].text.trim()
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.HOLD, remoteRobot)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.RELEASE, remoteRobot)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.SUBMIT, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to cancel one job and check that correct info is returned.
     */
    @Test
    fun testHoldCancelOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.INPUT, remoteRobot)
        Thread.sleep(2000)
        val jobId = find<ContainerFixture>(jobSubmittedIdLoc).findAllText()[2].text.trim()
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)

        doActionOnRunningJob(testInfo, JobAction.HOLD, remoteRobot)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(testInfo, JobAction.CANCEL, remoteRobot)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to release, then to cancel one job and check that correct info is returned.
     */
    @Test
    fun testHoldReleaseCancelOneJob(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJobWithMock(testInfo, datasetName, jobName, JobStatus.INPUT, remoteRobot)
        Thread.sleep(2000)
        val jobId = find<ContainerFixture>(jobSubmittedIdLoc).findAllText()[2].text.trim()
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.HOLD, remoteRobot)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.RELEASE, remoteRobot, JobStatus.INPUT)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId, remoteRobot)
        doActionOnRunningJob(testInfo, JobAction.CANCEL, remoteRobot)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Closes notifications and jobs tabs in jobs panel if exists.
     */
    private fun closeNotificationsAndJobsTabsIfExist(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            try {
                find<JLabelFixture>(notificationTitle).click()
                find<ComponentFixture>(closeNotificationLoc).click()
            } catch (ex: Exception) {
                when (ex) {
                    !is WaitForConditionTimeoutException -> throw ex
                }
            }

            try {
                findAll<ComponentFixture>(jobConsoleHeaderLoc).forEach {
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
     * Checks jobs result.
     */
    private fun checkJobResultInConsole(action: JobAction, jobId: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val rc = when (action) {
                JobAction.CANCEL -> "CANCELED"
                JobAction.SUBMIT -> RC_0000
                else -> throw Exception("Unknown action")
            }
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                waitFor(Duration.ofSeconds(20)) {
                    find<ComponentFixture>(editorLoc).hasText(jobExecutedConsoleText.format(jobName, jobId))

                }
                find<ComponentFixture>(editorLoc).findText(jobExecutedConsoleText.format(jobName, jobId))
                find<ComponentFixture>(editorLoc).findText(jobOwnerConsoleText.format(ZOS_USERID.uppercase()))
                find<ComponentFixture>(editorLoc).findText(jobReturnCodeConsoleText.format(rc))
            }
        }

    /**
     * Closes tab in jobs panel.
     */
    private fun closeJobTabInJobsPanel(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            findAll<ComponentFixture>(jobConsoleHeaderLoc).last()
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
    ){
            responseDispatcher.removeAllEndpoints()
            val myAction: String
            when (action) {
                JobAction.CANCEL -> {
                    myAction = buttonCancelActionName
                    injectJobToCanceled(testInfo, jobName)
                    injectJobLogHttp(testInfo, jobName, spoolFileName )
                    injectJobLog2Records(testInfo, jobName, filePath, dirFileNameForCancelSpool)
                    injectJobLogCanceledJobNotHttp(testInfo, jobName)
                }

                JobAction.HOLD -> {
                    myAction = buttonHoldActionName
                    injectJobLogHoldOrReleased(testInfo,jobName, holdWord)
                    injectJobLogHoldOrReleasedJobNotHttp(testInfo, jobName)
                }

                JobAction.RELEASE -> {
                    myAction = buttonReleaseActionName
                    injectJobToRelease(testInfo, jobName)
                    if (jobStatus == JobStatus.OUTPUT) {
                        injectJobLogHttp(testInfo, jobName, spoolFileName)
                        injectJobLog2Records(testInfo, jobName, filePath, dirFileNameForFileContent)
                        injectJobLogHttp(testInfo, jobName,  statusFileName)
                        injectJobLogHoldOrReleasedJobNotHttpAsSubmit(testInfo, jobName, statusFileName)

                    } else {
                        injectJobLogHoldOrReleasedJobNotHttp(testInfo, jobName)
                    }
                }

                else -> throw Exception("Unknown action")
            }

            ButtonElement(byXpath(actionLocPattern.format(myAction)), fixtureStack, remoteRobot).click()
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
            JobAction.CANCEL -> jobCancelNotification.format(jobName, jobId)
            JobAction.HOLD -> jobHoldNotification.format(jobName, jobId)
            JobAction.RELEASE -> jobReleseNotification.format(jobName, jobId)
            JobAction.SUBMIT -> jobSubmitNotification.format(jobName)
        }
        waitFor(Duration.ofSeconds(30)) {
            findAll<JLabelFixture>(notificationTitle).last().hasText(textToFind)
        }
        ButtonElement(closeNotificationLoc,fixtureStack,remoteRobot).click()
    }


    /**
     * Creates member in dataset and pastes content from buffer.
     */
    private fun createMemberAndPasteContentWithMock(
        testInfo: TestInfo, datasetName: String,
        memberName: String, fileName: String, remoteRobot: RemoteRobot
    ) {
        listMembersInDataset.add(memberName)
        injectMemberFromFile(testInfo, datasetName, memberName, filePath, fileName)
        injectSingleMember(testInfo, datasetName,  listMembersInDataset)
        utilObject.refreshWorkSpace(datasetName, fixtureStack, remoteRobot)
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
        val statusJson: String = when (jobStatus) {
            JobStatus.ACTIVE -> "getActiveStatus"
            else -> "getStatus"
        }
        injectJobToSubmit(testInfo, jobName, jobStatus,"{\"file\":\"//'$datasetName($jobName)'\"}")
        injectJobDetailsFromJson(testInfo, jobName, UNIVERSAL_JOB_ID)

        injectSpoolTextFromFile(testInfo, jobName, UNIVERSAL_JOB_ID, filePath, SPOOL_FILE_CONTENT)

        injectJobDetailsFromJsonNotHttp(testInfo, jobName, statusJson, jobStatus)
        submitJob(jobName, fixtureStack, remoteRobot)
    }

}
