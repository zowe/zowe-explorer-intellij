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
import com.intellij.remoterobot.utils.keyboard
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import workingset.PROJECT_NAME
import java.awt.event.KeyEvent
import java.io.File

/**
 * Tests submitting jobs and checks results.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class SubmitJobTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
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

    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(
            testInfo,
            connectionName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot
        )
        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${datasetName}_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${datasetName}") ?: false },
            { MockResponse().setBody("{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
        )
        allocatePDSAndCreateMask(
            wsName,
            datasetName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot,
            datasetName,
            1,
            false
        )
        mapListDatasets[datasetName] = listDS(datasetName, "PDS", "PO")
        openWSAndListDatasets(testInfo, remoteRobot)
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
        deleteDataset(datasetName, fixtureStack, remoteRobot)
        mockServer.shutdown()
        clearEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            close()
        }
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
     * Tests to submit job with RC=0, checks that correct info is returned.
     */
    @Test
    @Order(1)
    fun testSubmitJobWithRC0(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        doSubmitJobTest(testInfo, "TEST1", "job_rc0.txt", "CC 0000", remoteRobot)
    }

    /**
     * Tests to submit job with JCL ERROR, checks that correct info is returned.
     */
    @Test
    @Order(2)
    fun testSubmitJobWithJclError(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        doSubmitJobTest(testInfo, "TEST2", "job_jcl_error.txt", "JCL ERROR", remoteRobot)
    }

    /**
     * Tests to submit job with ABEND, checks that correct info is returned.
     */
    @Test
    @Order(3)
    fun testSubmitJobWithAbend(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        doSubmitJobTest(testInfo, "TEST3", "job_abend_s806.txt", "ABEND S806", remoteRobot)
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to open and close jobs outputs in explorer.
     */
    @Test
    @Order(4)
    fun testOpenAndCloseJobsOutputsInExplorer(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val jobName = "TEST1"
        val filter = Triple(jobName, ZOS_USERID, "")
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs",
            {
                it?.requestLine?.contains("/zosmf/restjobs/jobs?owner=${ZOS_USERID.uppercase()}&prefix=${jobName}")
                    ?: false
            },
            { MockResponse().setBody(replaceInJson("getJob", mapOf(Pair("hostName", mockServer.hostName),
                    Pair("port", mockServer.port.toString()), Pair("jobName", jobName), Pair("retCode", "CC 0000"),
                    Pair("jobStatus", "OUTPUT")))).setResponseCode(200) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs2",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/${jobName}/JOB07380/files HTTP") ?: false },
            { MockResponse().setBody(replaceInJson("getSpoolFiles", mapOf(Pair("hostName", mockServer.hostName),
                    Pair("port", mockServer.port.toString()), Pair("jobName", jobName), Pair("retCode", "CC 0000"),
                    Pair("jobStatus", "OUTPUT")))).setResponseCode(200) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName, ZOS_USERID, filter)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        Thread.sleep(5000)
        openJobsOutputs(testInfo, jobName, remoteRobot)
    }

    /**
     * Opens and closes the jobs outputs.
     */
    private fun openJobsOutputs(testInfo: TestInfo, jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val fileList = listOf("JESMSGLG", "JESJCL", "JESYSMSG")
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs3",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/${jobName}/JOB07380/files/") ?: false },
            { MockResponse().setBody("").setResponseCode(200) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            if (find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(jobName) }.size > 1) {
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(jobName) }.first().doubleClick()
            }

            Thread.sleep(2000)
            fileList.forEach { fileName ->
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(fileName) }.first().doubleClick()
                with(textEditor()) {
                    keyboard {
                        Thread.sleep(3000)
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                    }
                }
            }
        }
    }

    /**
     * Creates the member in dataset, submits it, checks that correct info is returned.
     */
    private fun doSubmitJobTest(
        testInfo: TestInfo,
        jobName: String,
        fileName: String,
        rc: String,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            openLocalFileAndCopyContent(filePath + fileName, fixtureStack, remoteRobot)
            Thread.sleep(3000)
            createMemberAndPasteContentWithMock(testInfo, datasetName, jobName, fileName, remoteRobot)
            submitJobWithMock(testInfo, datasetName, jobName, rc, remoteRobot)
            Thread.sleep(5000)
            val jobId = getJobIdFromPanel(remoteRobot)
            checkNotification(jobName, remoteRobot)
            checkTabPanelAndConsole(jobName, jobId, rc, remoteRobot)
        }

    private fun submitJobWithMock(
        testInfo: TestInfo,
        datasetName: String,
        jobName: String,
        rc: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val spoolFileContent = when (jobName) {
            "TEST1" -> {
                "mock/getSpoolFileContentRC00.txt"
            }

            "TEST2" -> {
                "mock/getSpoolFileContentJCLError.txt"
            }

            else -> {
                "mock/getSpoolFileContentAbend.txt"
            }
        }
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}__restjobs",
            { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs") ?: false },
            {
                MockResponse().setBody("{\"file\":\"//'$datasetName($jobName)'\"}")
                    .setBody(setBodyJobSubmit(jobName, JobStatus.ACTIVE))
            }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs_files",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files HTTP") ?: false },
            { MockResponse().setBody(replaceInJson("getSpoolFiles", mapOf(Pair("hostName", mockServer.hostName),
                    Pair("port", mockServer.port.toString()), Pair("jobName", jobName), Pair("retCode", rc),
                    Pair("jobStatus", "")))) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs_files2",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files/2/records") ?: false },
            { MockResponse().setBody(File(filePath + spoolFileContent).readText(Charsets.UTF_8)) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs_files3",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: false },
            { MockResponse().setBody(replaceInJson("getStatus", mapOf(Pair("hostName", mockServer.hostName),
                    Pair("port", mockServer.port.toString()), Pair("jobName", jobName), Pair("retCode", rc),
                    Pair("jobStatus", "OUTPUT")))) }
        )
        submitJob(jobName, fixtureStack, remoteRobot)
    }

    private fun createMemberAndPasteContentWithMock(
        testInfo: TestInfo, datasetName: String,
        memberName: String, fileName: String, remoteRobot: RemoteRobot
    ) {
        var isFirstRequest = true
        listMembersInDataset.add(memberName)
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${memberName}in${datasetName}_restfiles",
            { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberName})") ?: false && isFirstRequest },
            { MockResponse().setBody("") }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_fill_${memberName}_restfiles",
            { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberName})") ?: false && !isFirstRequest },
            { MockResponse().setBody(File(filePath + fileName).readText(Charsets.UTF_8)) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_listmembers1",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false },
            { MockResponse().setBody(buildListMembersJson()) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_getmember1",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}($memberName)") ?: false },
            { MockResponse().setBody("") }
        )

        createEmptyDatasetMember(datasetName, memberName, fixtureStack, remoteRobot)
        isFirstRequest = false
        pasteContent(memberName, fixtureStack, remoteRobot)
    }


    /**
     * Checks TabPanel and Console that correct info is returned.
     */
    private fun checkTabPanelAndConsole(jobName: String, jobId: String, rc: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "JOB $jobName($jobId) EXECUTED"
                )
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "OWNER: ${ZOS_USERID.uppercase()}"
                )
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "RETURN CODE: $rc"
                )
                find<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).findText(
                    "//'$datasetName($jobName)'"
                ).click()
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
                }
            }
        }

    /**
     * Gets jobId for submitted job.
     */
    private fun getJobIdFromPanel(remoteRobot: RemoteRobot): String = with(remoteRobot) {
        var jobId = ""
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            jobId = find<ContainerFixture>(byXpath("//div[@class='Tree']")).findAllText()[2].text.trim()
        }
        return jobId
    }

    /**
     * Checks notification that correct info is returned.
     */
    private fun checkNotification(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText("Job $jobName has been submitted")
                .click()
            find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
        }
    }

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

    private fun buildListMembersJson(): String {
        var members = "[ "
        if (listMembersInDataset.isNotEmpty()) {
            listMembersInDataset.forEach { members += "{\"member\": \"${it}\"}," }
        }
        members = members.dropLast(1) + "]"
        return "{\"items\":$members,\"returnedRows\": ${listMembersInDataset.size},\"JSONversion\": 1}"
    }

    private fun openWSAndListDatasets(testInfo: TestInfo, remoteRobot: RemoteRobot) {
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
        openOrCloseWorkingSetInExplorer(wsName, fixtureStack, remoteRobot)
    }
}