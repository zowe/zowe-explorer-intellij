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
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import testutils.ProcessManager
import workingset.*
import workingset.auxiliary.components.dialogs.AddJesWorkingSetSubDialog
import workingset.auxiliary.components.dialogs.SettingsDialogUtil
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.*
import workingset.jobConsolePurgeButton
import workingset.jobSubmittedIdLoc
import workingset.subOkButtonLoc

/**
 * Tests purging jobs and check results.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class PurgeJobTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog"
    )
    var okButton = ButtonElement()
    private var purgeJobActionButton = ButtonElement()
    private var utilObject = IdeaInteractionClass()
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val jwsName = "JWS1"
    private val datasetName = "$ZOS_USERID.PURGE.JOBS".uppercase()
    private var okButtonSub = ButtonElement()

    private var settingsDialog = SettingsDialogUtil()
    private var addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog()


    private val filesList = arrayOf("job_rc0.txt", "job_jcl_error.txt", "job_abend_s806.txt")
    private val memberName = arrayOf("TEST1", "TEST2", "TEST3")
    private val memberNameFileName = memberName.associateWith { filesList[memberName.indexOf(it)] }
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/"
    private val filters = listOf( Triple(memberName[0], ZOS_USERID, ""), Triple(memberName[1], ZOS_USERID, ""), Triple(memberName[2], ZOS_USERID, ""))

    private var mapListDatasets = mutableMapOf<String, String>()
    private var listMembersInDataset = mutableListOf<String>()
    private var datasetParams = "{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}"
    private var testAbendMap = mapOf("TEST1" to "CC 0000", "TEST2" to "JCL ERROR", "TEST3" to "ABEND S806")
    private var testAbendFileRcMap = mapOf(
            "TEST1" to listOf("mock/getSpoolFileContentRC00.txt","CC 0000"),
            "TEST2" to listOf("mock/getSpoolFileContentJCLError.txt", "JCL ERROR"),
            "TEST3" to listOf("mock/getSpoolFileContentAbend.txt", "ABEND S806")
    )
    private lateinit var processManager: ProcessManager


    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(
            testInfo,
            connectionName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot
        )

        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        purgeJobActionButton = ButtonElement(jobConsolePurgeButton, fixtureStack, remoteRobot)
        settingsDialog = SettingsDialogUtil(fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog(fixtureStack, remoteRobot)
        okButtonSub = ButtonElement(subOkButtonLoc, fixtureStack, remoteRobot)

        injectTestInfoForPdsDataset(testInfo.displayName, datasetParams, datasetName)

        mapListDatasets[datasetName] = listDS(datasetName, PDS_TYPE, PO_ORG_SHORT)

        injectListAllAllocatedDatasetsWithContents(testInfo, datasetName, mapListDatasets)
        injectJobList(testInfo, datasetName, listMembersInDataset)

        utilObject.createWsAndMask(
            wsName,
            connectionName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot,
            Pair(datasetName, ZOS_MASK)
        )

        memberNameFileName.forEach {
            openLocalFileAndCopyContent(filePath + it.value, fixtureStack, remoteRobot)
            listMembersInDataset.add(it.key)
            injectMemberFromFile(testInfo, datasetName, it.key, filePath, it.value)
            injectJobList(testInfo, datasetName, listMembersInDataset)
        }
        utilObject.compressAndDecompressTree(wsName, fixtureStack, remoteRobot)

        utilObject.createJWS(jwsName, connectionName, filters, fixtureStack, closableFixtureCollector, remoteRobot)
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
    fun tearDown(remoteRobot: RemoteRobot) {
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests to purge jobs with RC=0, JCL ERROR, ABEND via action button in jobs terminal.
     * Checks that correct info is returned.
     */
//    @Test
    @ParameterizedTest
    @ValueSource(strings = ["TEST1", "TEST2", "TEST3"])
    fun testPurgeJobViaActionButtonInTerminal(
        memberName: String,testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot){
        val name = injectSaccessedPurge(testInfo,memberName)

        submitJobWithMock(testInfo, datasetName, memberName, remoteRobot)
        Thread.sleep(2000)
        // can not wait until text available
        val jobId = find<ContainerFixture>(jobSubmittedIdLoc).findAllText()[2].text.trim()
        utilObject.checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, memberName, remoteRobot)

        purgeJobActionButton.click()
        utilObject.checkNotificationAfterJobAction(JobAction.PURGED, jobId, memberName, remoteRobot)

        responseDispatcher.removeEndpoint(name)
        injectFailPurge(testInfo, memberName)

        purgeJobActionButton.click()
        utilObject.checkNotificationAfterJobAction(JobAction.ERROR_PURGED, jobId, memberName, remoteRobot)
        utilObject.closeTabInJobsPanel(datasetName, memberName, remoteRobot)
    }


    /**
     * Tests to purge jobs with RC=0, JCL ERROR, ABEND via context menu in Jes Explorer.
     * Checks that correct info is returned.
     */
    @ParameterizedTest
    @ValueSource(strings = ["TEST1", "TEST2", "TEST3"])
    fun testPurgeJobViaContextMenuInExplorer(memberName: String,testInfo: TestInfo, remoteRobot: RemoteRobot)

        =  with(remoteRobot) {

            submitJobWithMock(testInfo, datasetName, memberName, remoteRobot)
            Thread.sleep(2000)
            val jobId = find<ContainerFixture>(jobSubmittedIdLoc).findAllText()[2].text.trim()
            utilObject.checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, memberName, remoteRobot)

            utilObject.closeTabInJobsPanel(datasetName, memberName, remoteRobot)

            purgeJobFromExplorer(testInfo, memberName, jobId, remoteRobot)

            utilObject.checkNotificationAfterJobAction(JobAction.PURGED, jobId, memberName, remoteRobot)
            shouldThrow<NoSuchElementException> {find<ComponentFixture>(viewTree).findText("$memberName ($jobId)")}

            shouldThrow<NoSuchElementException> {
                find<ComponentFixture>(viewTree).findText("$memberName ($jobId)")
            }
            utilObject.compressAndDecompressTree(convertJobFilterToString(Triple(memberName, ZOS_USERID, "")),fixtureStack, remoteRobot)


    }

    /**
     * Purges job via context menu.
     */
    private fun purgeJobFromExplorer(testInfo: TestInfo, jobName: String, jobId: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {

            val rc: String = testAbendMap[jobName].toString()

            injectSaccessedPurge(testInfo,jobName)

            val injectedNameBeforeDeleted = injectOwnerPrefixJobDetails(testInfo, jobName, ZOS_USERID.uppercase(),rc)
            injectOwnerPrefixJobNoDetails(testInfo,jobName,ZOS_USERID.uppercase())

            injectJobLogHttp(testInfo, jobName, FILE_NAME_GET_SPOOL, rc)

            utilObject.openJesExplorerTab(fixtureStack, remoteRobot)
            utilObject.openJobFilterInExplorer(Triple(jobName, ZOS_USERID, ""), "", fixtureStack, remoteRobot)

            responseDispatcher.removeEndpoint(injectedNameBeforeDeleted)
            utilObject.callPurgeJob(jobName,jobId, remoteRobot)

        }

    /**
     * Submits job on mock server and sets OUTPUT job status.
     */
    private fun submitJobWithMock(
        testInfo: TestInfo,
        datasetName: String,
        jobName: String,
        remoteRobot: RemoteRobot
    ) {
        val spoolFileContent:String = testAbendFileRcMap[jobName]?.get(0)?:""
        val rc:String = testAbendFileRcMap[jobName]?.get(1)?:""

        injectJobToSubmit(testInfo, jobName, JobStatus.ACTIVE, FILE_DATASET_NAME_JOB_NAME.format(datasetName,jobName))
        injectJobLogHttp(testInfo, jobName, FILE_NAME_GET_SPOOL, rc)
        injectJobLog2Records(testInfo, jobName, filePath, spoolFileContent)
        injectJobLogHoldOrReleasedJobNotHttpAsSubmit(testInfo, jobName, FILE_NAME_GET_STATUS, rc)

        submitJob(jobName, fixtureStack, remoteRobot)
    }
}
