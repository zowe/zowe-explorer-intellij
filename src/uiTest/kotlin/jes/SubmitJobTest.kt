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
import com.intellij.remoterobot.utils.keyboard
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import testutils.ProcessManager
import workingset.*
import workingset.testutils.*
import workingset.editorLoc
import workingset.jobConsoleHeaderLoc
import workingset.jobSubmittedIdLoc
import java.awt.event.KeyEvent

/**
 * Tests submitting jobs and checks results.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class SubmitJobTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var utilObject = IdeaInteractionClass()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog"
    )
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val datasetName = "$ZOS_USERID.SUBMIT.JOBS".uppercase()
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/"
    private var mapListDatasets = mutableMapOf<String, String>()
    private var listMembersInDataset = mutableListOf<String>()
    private val filesList = arrayOf("job_rc0.txt", "job_jcl_error.txt", "job_abend_s806.txt")
    private val memberName = arrayOf("TEST1", "TEST2", "TEST3")
    private val memberNameFileName = memberName.associateWith { filesList[memberName.indexOf(it)] }
    private var testAbendFileRcMap = mapOf(
            "TEST1" to listOf("mock/getSpoolFileContentRC00.txt","CC 0000"),
            "TEST2" to listOf("mock/getSpoolFileContentJCLError.txt", "JCL ERROR"),
            "TEST3" to listOf("mock/getSpoolFileContentAbend.txt", "ABEND S806")
    )
    private var datasetParams = "{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":2,\"dirblk\":1,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}"

    private val jwsFileList = listOf("JESMSGLG", "JESJCL", "JESYSMSG")
    private val jwsName = "JWS1"
    private val jobName = "TEST1"
    private val filter = Triple(jobName, ZOS_USERID, "")
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
        injectTestInfoForPdsDataset(testInfo.displayName, datasetParams,datasetName)

        mapListDatasets[datasetName] = listDS(datasetName, PDS_TYPE, PO_ORG_SHORT)

        injectListAllAllocatedDatasetsWithContents(testInfo,datasetName,mapListDatasets)
        injectJobList(testInfo,datasetName,listMembersInDataset)

        utilObject.createWsAndMask(wsName, connectionName, fixtureStack, closableFixtureCollector,remoteRobot,  Pair(datasetName, ZOS_MASK))

        memberNameFileName.forEach {
            openLocalFileAndCopyContent(filePath + it.value, fixtureStack, remoteRobot)
            listMembersInDataset.add(it.key)
            injectMemberFromFile(testInfo, datasetName, it.key, filePath, it.value)
            injectJobList(testInfo,datasetName,listMembersInDataset)
        }
        utilObject.compressAndDecompressTree(wsName,fixtureStack,remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll() {
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

    @ParameterizedTest
    @ValueSource(strings = ["TEST1", "TEST2", "TEST3"])
    fun testSubmitJob(memberName: String,testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot){
        submitJobWithMock(testInfo, datasetName, memberName, remoteRobot)
        Thread.sleep(2000)
        val jobId = find<ContainerFixture>(jobSubmittedIdLoc).findAllText()[2].text.trim()
        utilObject.checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, memberName, remoteRobot)
        checkTabPanelAndConsole(memberName, jobId, remoteRobot)
    }

    /**
     * Tests to open and close jobs outputs in explorer.
     */
    @Test
    fun testOpenAndCloseJobsOutputsInExplorer(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectOwnerPrefixJobDetails(testInfo,jobName,ZOS_USERID.uppercase())
        injectJobLogHttp(testInfo,jobName,FILE_NAME_GET_SPOOL)
        utilObject.createJWS(jwsName,connectionName,filter, fixtureStack, closableFixtureCollector, remoteRobot)

        openJobsOutputs(testInfo, jobName, remoteRobot)
    }

    /**
     * Opens and closes the jobs outputs.
     */
    private fun openJobsOutputs(testInfo: TestInfo, jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectEmptySpoolFiles(testInfo,jobName)
        utilObject.refreshWorkSpace(jwsName,fixtureStack,remoteRobot)
        if (find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(jobName) }.size > 1) {
            find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(jobName) }.first().doubleClick()
        }
            jwsFileList.forEach { fileName ->
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(fileName) }.first().doubleClick()
                closeTabByKeyboard(fixtureStack,remoteRobot)
            }
    }

    private fun submitJobWithMock(
        testInfo: TestInfo,
        datasetName: String,
        jobName: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {

        val spoolFileContent:String = testAbendFileRcMap[jobName]?.get(0)?:""
        val rc:String = testAbendFileRcMap[jobName]?.get(1)?:""
        injectJobToSubmit(testInfo,jobName,JobStatus.ACTIVE,FILE_DATASET_NAME_JOB_NAME.format(datasetName,jobName))
        injectJobLogHttp(testInfo,jobName,FILE_NAME_GET_SPOOL,rc)

        injectJobLog2Records(testInfo,jobName,filePath,spoolFileContent)

        injectJobLogHoldOrReleasedJobNotHttpAsSubmit(testInfo,jobName,FILE_NAME_GET_STATUS,rc)

        submitJob(jobName, fixtureStack, remoteRobot)
    }

    /**
     * Checks TabPanel and Console that correct info is returned.
     */
    private fun checkTabPanelAndConsole(jobName: String, jobId: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val rc:String = testAbendFileRcMap[jobName]?.get(1)?:""
            find<ComponentFixture>(editorLoc).findText(jobExecutedConsoleText.format(jobName,jobId))
            find<ComponentFixture>(editorLoc).findText(jobOwnerConsoleText.format(ZOS_USERID.uppercase()))
            find<ComponentFixture>(editorLoc).findText(jobReturnCodeConsoleText.format(rc))

            find<ComponentFixture>(jobConsoleHeaderLoc).findText("//'$datasetName($jobName)'").click()
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
            }
        }
}
