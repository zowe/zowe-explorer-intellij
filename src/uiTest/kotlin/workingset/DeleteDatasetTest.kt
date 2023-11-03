/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package workingset

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Tests allocating datasets with valid and invalid inputs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class DeleteDatasetTest:WorkingSetBase() {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog"
    )
    private val projectName = "untitled"
    override val connectionName = "valid connection"

    val wsName = "WS1"
    override val datasetName = "$ZOS_USERID.ALLOC."
    private val recordFormats = mutableListOf("F", "FB", "V", "VA", "VB")
    private var datasetsToBeDeleted = mutableListOf<String>()

       private var mapListDatasets = mutableMapOf<String, String>()

    /**
     * Opens the project and Explorer, clears test environment, creates working set and mask.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
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
        createWsAndMask(remoteRobot)

    }

    /**
     * Closes the project and clears test environment, deletes created datasets.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        deleteDatasets(remoteRobot)
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

    //todo add tests with advanced parameters when switched to mock

    /**
     * Tests to allocate PO datasets with valid parameters.
     */
    @ParameterizedTest
    @ValueSource(strings = ["Sequential (PS)", "Partitioned (PO)", "Partitioned Extended (PO-E)"])
    fun testDeleteDatasets(input: String, remoteRobot: RemoteRobot) {
        doValidTest(input, remoteRobot)
        deleteDatasets(remoteRobot)
    }

    /**
     * Allocates dataset with different record formats.
     */
    private fun doValidTest(datasetOrganization: String, remoteRobot: RemoteRobot) {
        recordFormats.forEach { s ->
            val recordLength = if (s == "F") {
                3200
            } else {
                80
            }
//            val dsName = "${datasetName}${datasetOrganization.replace("-", "")}.${s}".uppercase()
            val dsOrganisationShort = "\\((.*?)\\)".toRegex().find(datasetOrganization)?.groupValues?.get(1)
            val dsName = "${datasetName}${dsOrganisationShort}.${s}".uppercase().replace("-", "")
            responseDispatcher.injectEndpoint(
                "testAllocateValid${datasetOrganization}Datasets_${s}_restfiles",
                { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${dsName}") ?: false },
                { MockResponse().setBody("{\"dsorg\":\"${dsOrganisationShort}\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"${s}\",\"blksize\":3200,\"lrecl\":${recordLength}}") }
            )
//            Thread.sleep(3000)
            allocateDataSet(
                wsName, dsName, datasetOrganization, "TRK", 10, 1, 1,
                s, recordLength, 3200, 0, remoteRobot
            )

            val dsntp = if (dsOrganisationShort == "PS") {
                ""
            } else {
                "PDS"
            }

            val listDs = "{\n" +
                    "      \"dsname\": \"${dsName}\",\n" +
                    "      \"blksz\": \"3200\",\n" +
                    "      \"catnm\": \"TEST.CATALOG.MASTER\",\n" +
                    "      \"cdate\": \"2021/11/15\",\n" +
                    "      \"dev\": \"3390\",\n" +
                    "      \"dsntp\": \"${dsntp}\",\n" +
                    "      \"dsorg\": \"${datasetOrganization}\",\n" +
                    "      \"edate\": \"***None***\",\n" +
                    "      \"extx\": \"1\",\n" +
                    "      \"lrecl\": \"${recordLength}\",\n" +
                    "      \"migr\": \"NO\",\n" +
                    "      \"mvol\": \"N\",\n" +
                    "      \"ovf\": \"NO\",\n" +
                    "      \"rdate\": \"2021/11/17\",\n" +
                    "      \"recfm\": \"${s}\",\n" +
                    "      \"sizex\": \"10\",\n" +
                    "      \"spacu\": \"TRACKS\",\n" +
                    "      \"used\": \"1\",\n" +
                    "      \"vol\": \"TESTVOL\",\n" +
                    "      \"vols\": \"TESTVOL\"\n" +
                    "    },"
            mapListDatasets[dsName] = listDs
            datasetsToBeDeleted.add(dsName)
        }
    }

    /**
     * Deletes created datasets.
     */
    private fun deleteDatasets(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllAllocatedDatasets_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetName.uppercase()}*") ?: false },
            { MockResponse().setBody(buildFinalListDatasetJson()) }
        )
        responseDispatcher.injectEndpoint(
            "listMembers_restfiles",
            { it?.requestLine?.contains("/member") ?: false },
            { MockResponse().setBody("{\"items\":[],\"returnedRows\":0,\"totalRows\":0,\"moreRows\":null,\"JSONversion\":1}") }
        )
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenuItem(remoteRobot, "Refresh").click()
            explorer {
                find<ComponentFixture>(viewTree).findAllText(wsName).last().doubleClick()
            }
        }
//                    Thread.sleep(1000)
//        Thread.sleep(3000)
        datasetsToBeDeleted.forEach { s ->
            mapListDatasets.remove(s)
            responseDispatcher.injectEndpoint(
                "listAllocatedDatasets_restfiles_${s}",
                { it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetName.uppercase()}*") ?: false },
                { MockResponse().setBody(buildFinalListDatasetJson()) }
            )
            responseDispatcher.injectEndpoint(
                "deleteDataset_restfiles_${s}",
                { it?.requestLine?.contains("DELETE /zosmf/restfiles/ds/${s}") ?: false },
                { MockResponse().setBody("{}") }
            )
            deleteDataset(s, projectName, fixtureStack, remoteRobot)

        }
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findAllText(wsName).last().doubleClick()
//                Thread.sleep(1000)
            }
        }
        datasetsToBeDeleted.clear()
    }



//    /**
//     * Creates working set and z/OS mask.
//     */
//    private fun createWsAndMask(remoteRobot: RemoteRobot) = with(remoteRobot) {
//        createWsWithoutMask(projectName, wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
//        ideFrameImpl(projectName, fixtureStack) {
//            createMask(wsName, fixtureStack, closableFixtureCollector)
//            createMaskDialog(fixtureStack) {
//                createMask(Pair("$datasetName*", "z/OS"))
//                clickButton("OK")
//            }
//            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
//        }
//    }
}