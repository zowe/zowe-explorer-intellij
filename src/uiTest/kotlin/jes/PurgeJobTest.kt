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
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import io.kotest.assertions.throwables.shouldThrow
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.event.KeyEvent
import java.io.File

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
    private val projectName = "untitled"
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val jwsName = "JWS1"
    private val datasetName = "$ZOS_USERID.PURGE.JOBS".uppercase()

    private val filesList = mutableListOf("job_rc0.txt", "job_jcl_error.txt", "job_abend_s806.txt")
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/"
    private var mapListDatasets = mutableMapOf<String, String>()
    private var listMembersInDataset = mutableListOf<String>()

    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        startMockServer()
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_info",
            { it?.requestLine?.contains("zosmf/info") ?: false },
            { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_resttopology",
            { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
            { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
        )
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(
            projectName,
            fixtureStack,
            closableFixtureCollector,
            connectionName,
            true,
            remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
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
        createJobs(testInfo, remoteRobot)
        createJWS(remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(testInfo: TestInfo,remoteRobot: RemoteRobot) = with(remoteRobot) {
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
    fun tearDown(remoteRobot: RemoteRobot) {
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests to purge jobs with RC=0, JCL ERROR, ABEND via action button in jobs terminal.
     * Checks that correct info is returned.
     */
    @Test
    @Order(1)
    fun testPurgeJobViaActionButtonInTerminal(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        for (i in 1..3) {
            doPurgeJobTestViaActionButton(testInfo, "TEST$i", remoteRobot)
        }
    }


    /**
     * Tests to purge jobs with RC=0, JCL ERROR, ABEND via context menu in Jes Explorer.
     * Checks that correct info is returned.
     */
     @Test
    @Order(2)
    fun testPurgeJobViaContextMenuInExplorer(testInfo: TestInfo,remoteRobot: RemoteRobot) {
        for (i in 1..3) {
            doPurgeJobTestViaContextMenu(testInfo,"TEST$i", remoteRobot)
        }
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Creates working set.
     */
    private fun createJWS(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val filters = mutableListOf(
            Triple("TEST1", ZOS_USERID, ""),
            Triple("TEST2", ZOS_USERID, ""),
            Triple("TEST3", ZOS_USERID, "")
        )
        ideFrameImpl(projectName, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, filters)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
    }

    /**
     * Purges job via action button, checks notifications.
     */
    private fun doPurgeJobTestViaActionButton(testInfo: TestInfo, jobName: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            var isFirstRequest = true
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_delete_${jobName}",
                { it?.requestLine?.contains("DELETE /zosmf/restjobs/jobs/${jobName}/JOB07380") ?: false && isFirstRequest},
                { MockResponse().setBody("{}").setBody(setBodyJobPurged(jobName)) }
            )

            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_delete_${jobName}_2",
                { it?.requestLine?.contains("DELETE /zosmf/restjobs/jobs/${jobName}/JOB07380") ?: false && !isFirstRequest},
                { MockResponse().setBody("{}").setResponseCode(400) }
            )
            submitJobWithMock(testInfo, datasetName, jobName, remoteRobot)
            Thread.sleep(2000)
            val jobId = getJobIdFromPanel(remoteRobot)
            checkNotificationJobSubmitted(jobName, remoteRobot)
            purgeJobFromTerminal(remoteRobot)
            isFirstRequest = false
            Thread.sleep(2000)
            checkNotificationJobPurged(jobName, jobId, true, remoteRobot)
            purgeJobFromTerminal(remoteRobot)
            Thread.sleep(2000)
            checkNotificationJobPurged(jobName, jobId, false, remoteRobot)
            closeTabInJobsPanel(jobName, remoteRobot)
            responseDispatcher.removeAllEndpoints()
        }

    /**
     * Purges job via context menu, checks notifications.
     */
    private fun doPurgeJobTestViaContextMenu(testInfo: TestInfo,jobName: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {

            submitJobWithMock(testInfo, datasetName, jobName, remoteRobot)
            Thread.sleep(2000)
            val jobId = getJobIdFromPanel(remoteRobot)
            checkNotificationJobSubmitted(jobName, remoteRobot)
            closeTabInJobsPanel(jobName, remoteRobot)
            purgeJobFromExplorer(testInfo,jobName, jobId, remoteRobot)
            Thread.sleep(3000)
            checkNotificationJobPurged(jobName, jobId, true, remoteRobot)
            checkJobsOutputWasDeletedJWSRefreshed(jobName, jobId, remoteRobot)
            closeFilterInExplorer(Triple(jobName, ZOS_USERID, ""), projectName, fixtureStack, remoteRobot)
            responseDispatcher.removeAllEndpoints()
        }

    /**
     * Checks that job output was deleted after purging job.
     */
    private fun checkJobsOutputWasDeletedJWSRefreshed(
        jobName: String, jobId: String, remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                shouldThrow<NoSuchElementException> {
                    find<ComponentFixture>(viewTree).findText("$jobName ($jobId)")
                }
            }
        }
    }

    /**
     * Closes tab for job in jobs panel.
     */
    private fun closeTabInJobsPanel(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            find<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).findText(
                "//'$datasetName($jobName)'"
            ).click()
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
            }
        }
    }

    /**
     * Creates jobs in dataset.
     */
    private fun createJobs(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        var n = 1
        filesList.forEach {
            openLocalFileAndCopyContent(filePath + it, projectName, fixtureStack, remoteRobot)
            Thread.sleep(3000)
            createMemberAndPasteContentWithMock(testInfo, datasetName, "TEST$n", it, remoteRobot)
            n++
        }
    }

    /**
     * Creates working set.
     */
    private fun createWS(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    /**
     * Purges job via action button.
     */
    private fun purgeJobFromTerminal(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            clickActionButton(byXpath("//div[@class='ActionButton' and @myaction='Purge Job ()']"))
        }
    }

    /**
     * Purges job via context menu.
     */
    private fun purgeJobFromExplorer(testInfo: TestInfo,jobName: String, jobId: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        var isFirstRequest = true
        val rc = when (jobName) {
            "TEST1" -> {
                "CC 0000"
            }
            "TEST2" -> {
                "JCL ERROR"
            }
            else -> {
                "ABEND S806"
            }
        }
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_delete_${jobName}",
            { it?.requestLine?.contains("DELETE /zosmf/restjobs/jobs/${jobName}/JOB07380") ?: false},
            { MockResponse().setBody("{}").setBody(setBodyJobPurged(jobName)) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${jobName}_restjobs",
            {
                it?.requestLine?.contains("/zosmf/restjobs/jobs?owner=${ZOS_USERID.uppercase()}&prefix=${jobName}")
                        ?: false && isFirstRequest
            },
            { MockResponse().setBody(replaceInJson("getJob", jobName, rc,"OUTPUT")).setResponseCode(200) }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${jobName}_restjobs2",
            {
                it?.requestLine?.contains("/zosmf/restjobs/jobs?owner=${ZOS_USERID.uppercase()}&prefix=${jobName}")
                        ?: false && !isFirstRequest
            },
            { MockResponse().setBody("[]")}
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${jobName}_restjobs3",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/${jobName}/JOB07380/files HTTP") ?: false },
            { MockResponse().setBody(replaceInJson("getSpoolFiles", jobName, rc)).setResponseCode(200) }
        )
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                jesExplorer.click()
            }
        }
        openJobFilterInExplorer(Triple(jobName, ZOS_USERID, ""), "", projectName, fixtureStack, remoteRobot)
        isFirstRequest=false
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith("$jobName ($jobId)") }.first()
                    .rightClick()
            }
            actionMenuItem(remoteRobot, "Purge Job").click()
        }

    }


    /**
     * Gets jobId for submitted job.
     */
    private fun getJobIdFromPanel(remoteRobot: RemoteRobot): String = with(remoteRobot) {
        var jobId = ""
        ideFrameImpl(projectName, fixtureStack) {
            jobId = find<ContainerFixture>(byXpath("//div[@class='Tree']")).findAllText()[2].text.trim()
        }
        return jobId
    }

    /**
     * Checks notification that correct info is returned for submitted job.
     */
    private fun checkNotificationJobSubmitted(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText("Job $jobName has been submitted")
                .click()
            find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
        }
    }

    /**
     * Checks notification that correct info is returned for purged job.
     */
    private fun checkNotificationJobPurged(
        jobName: String,
        jobId: String,
        jobExist: Boolean,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val textToFind = if (jobExist) {
            "$jobName: $jobId has been purged"
        } else {
            "Error purging $jobName: $jobId"
        }
        ideFrameImpl(projectName, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText(textToFind)
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

        createEmptyMember(datasetName, memberName, remoteRobot)
        isFirstRequest = false
        pasteContent(memberName, remoteRobot)
        Thread.sleep(3000)
    }

    private fun createEmptyMember(
        datasetName: String,
        memberName: String,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    fileExplorer.click()
                    find<ComponentFixture>(viewTree).findAllText(datasetName).last().rightClick()
                }
                actionMenu(remoteRobot, "New").click()
                actionMenuItem(remoteRobot, "Member").click()
                dialog("Create Member") {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = memberName
                }
                clickButton("OK")
            }
        }

    private fun pasteContent(
        memberName: String,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    find<ComponentFixture>(viewTree).findAllText(memberName).last().doubleClick()
                    Thread.sleep(2000)
                }
                with(textEditor()) {
                    keyboard {
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_V)
                        Thread.sleep(2000)
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_S)
                        Thread.sleep(2000)
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                    }
                }
            }
        }

    private fun submitJobWithMock(
        testInfo: TestInfo,
        datasetName: String,
        jobName: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val spoolFileContent: String
        val rc: String
        when (jobName) {
            "TEST1" -> {
                spoolFileContent = "mock/getSpoolFileContentRC00.txt"
                rc = "CC 0000"
            }

            "TEST2" -> {
                spoolFileContent = "mock/getSpoolFileContentJCLError.txt"
                rc = "JCL ERROR"
            }

            else -> {
                spoolFileContent = "mock/getSpoolFileContentAbend.txt"
                rc = "ABEND S806"
            }
        }
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}__restjobs",
            { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs") ?: false },
            { MockResponse().setBody("{\"file\":\"//'$datasetName($jobName)'\"}").setBody(setBodyJobSubmit(jobName)) }
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
            { MockResponse().setBody(replaceInJson("getStatus", jobName, rc,"OUTPUT")) }
        )
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
    }

    private fun replaceInJson(fileName: String, jobName: String, rc: String,jobStatus:String=""): String {
        return (responseDispatcher.readMockJson(fileName) ?: "").replace("hostName", mockServer.hostName)
            .replace("port", mockServer.port.toString())
            .replace("jobName", jobName)
            .replace("retCode", rc).
                replace("jobStatus",jobStatus)
    }

    private fun setBodyJobSubmit(jobName: String): String {
        return "{\n" +
                "  \"owner\": \"${ZOS_USERID.uppercase()}\",\n" +
                "  \"phase\": 14,\n" +
                "  \"subsystem\": \"JES2\",\n" +
                "  \"phase-name\": \"Job is actively executing\",\n" +
                "  \"job-correlator\": \"J0007380S0W1....DB23523B.......:\",\n" +
                "  \"type\": \"JOB\",\n" +
                "  \"url\": \"https://${mockServer.hostName}:${mockServer.port}/zosmf/restjobs/jobs/J0007380S0W1....DB23523B.......%3A\",\n" +
                "  \"jobid\": \"JOB07380\",\n" +
                "  \"class\": \"B\",\n" +
                "  \"files-url\": \"https://${mockServer.hostName}:${mockServer.port}/zosmf/restjobs/jobs/J0007380S0W1....DB23523B.......%3A/files\",\n" +
                "  \"jobname\": \"${jobName}\",\n" +
                "  \"status\": \"ACTIVE\",\n" +
                "  \"retcode\": null\n" +
                "}\n"
    }

    private fun setBodyJobPurged(jobName:String):String{
        return "{\n" +
                "  \"jobid\":\"JOB07380\",\n" +
                "  \"jobname\":\"${jobName}\",\n" +
                "  \"original-jobid\":\"JOB07380\",\n" +
                "  \"owner\":\"${ZOS_USERID.uppercase()}\",\n" +
                "  \"member\":\"JES2\",\n" +
                "  \"sysname\":\"SY1\",\n" +
                "  \"job-correlator\":\"JOB07380SY1.....CC20F380.......:\",\n" +
                "  \"status\":\"0\"\n" +
                "}\n"
    }
}