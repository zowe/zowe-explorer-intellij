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
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

/**
 * Tests allocating datasets with valid and invalid inputs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class AllocateDatasetTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog"
    )
    private val projectName = "untitled"
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val datasetName = "$ZOS_USERID.ALLOC."
    private val recordFormats = mutableListOf("F", "FB", "V", "VA", "VB")
    private var datasetsToBeDeleted = mutableListOf<String>()

    //TODO change message when ijmp-907 is fixed
    private val numberGreaterThanOneMsg = "Enter a number grater than 1"
    private val enterPositiveNumberMsg = "Enter a positive number"
    private var mapListDatasets = mutableMapOf<String, String>()

    /**
     * Opens the project and Explorer, clears test environment, creates working set and mask.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo,remoteRobot: RemoteRobot) = with(remoteRobot) {
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
    @Test
    fun testAllocateValidPODatasets(remoteRobot: RemoteRobot) {
        doValidTest("PO", remoteRobot)
    }

    /**
     * Tests to allocate PS datasets with valid parameters.
     */
    @Test
    fun testAllocateValidPSDatasets(remoteRobot: RemoteRobot) {
        doValidTest("PS", remoteRobot)
    }

    /**
     * Tests to allocate POE datasets with valid parameters.
     */
    @Test
    fun testAllocateValidPOEDatasets(remoteRobot: RemoteRobot) {
        doValidTest("PO-E", remoteRobot)
    }

    /**
     * Tests to allocate dataset with invalid dataset name, checks that correct message is returned.
     */
    @Test
    fun testInvalidDatasetName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val message =
            "Each name segment (qualifier) is 1 to 8 characters, the first of which must be alphabetic (A to Z) or " +
                    "national (# @ $). The remaining seven characters are either alphabetic, numeric (0 - 9), national, " +
                    "a hyphen (-). Name segments are separated by a period (.)"
        allocateDataSet(
            wsName, "A23456789.A", "PO", "TRK", 10, 1, 1,
            "FB", 80, 3200, 0, remoteRobot, false, message
        )
    }

    /**
     * Tests to allocate dataset with invalid primary allocation, checks that correct message is returned.
     */
    @Test
    fun testInvalidPrimaryAllocation(remoteRobot: RemoteRobot) {
        allocateDataSet(
            wsName, "A23.A23", "PO", "TRK", -2, 0, 1,
            "FB", 80, 3200, 0, remoteRobot, false, numberGreaterThanOneMsg
        )
    }

    /**
     * Tests to allocate dataset with invalid directory, checks that correct message is returned.
     */
    @Test
    fun testInvalidDirectory(remoteRobot: RemoteRobot) {
        allocateDataSet(
            wsName, "A23.A23", "PO", "TRK", 10, 0, 0,
            "FB", 80, 3200, 0, remoteRobot, false, numberGreaterThanOneMsg
        )
    }

    /**
     * Tests to allocate dataset with invalid record length, checks that correct message is returned.
     */
    @Test
    fun testInvalidRecordLength(remoteRobot: RemoteRobot) {
        allocateDataSet(
            wsName, "A23.A23", "PO", "TRK", 10, 0, 1,
            "FB", 0, 3200, 0, remoteRobot, false, numberGreaterThanOneMsg
        )
    }

    /**
     * Tests to allocate dataset with invalid secondary allocation, checks that correct message is returned.
     */
    @Test
    fun testInvalidSecondaryAllocation(remoteRobot: RemoteRobot) {
        allocateDataSet(
            wsName, "A23.A23", "PO", "TRK", 10, -10, 1,
            "FB", 80, 3200, 0, remoteRobot, false, enterPositiveNumberMsg
        )
    }

    /**
     * Tests to allocate dataset with invalid block size, checks that correct message is returned.
     */
    @Test
    fun testInvalidBlockSize(remoteRobot: RemoteRobot) {
        allocateDataSet(
            wsName, "A23.A23", "PO", "TRK", 10, 0, 1,
            "FB", 80, -1, 0, remoteRobot, false, enterPositiveNumberMsg
        )
    }

    /**
     * Tests to allocate dataset with invalid average block length, checks that correct message is returned.
     */
    @Test
    fun testInvalidAverageBlockLength(remoteRobot: RemoteRobot) {
        allocateDataSet(
            wsName, "A23.A23", "PO", "TRK", 10, 0, 1,
            "FB", 80, 3200, -1, remoteRobot, false, enterPositiveNumberMsg
        )
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
            val dsName = "${datasetName}${datasetOrganization.replace("-", "")}.${s}".uppercase()
            responseDispatcher.injectEndpoint(
                "testAllocateValid${datasetOrganization}Datasets_${s}_restfiles",
                { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${dsName}") ?: false },
                { MockResponse().setBody("{\"dsorg\":\"${datasetOrganization}\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"${s}\",\"blksize\":3200,\"lrecl\":${recordLength}}") }
            )
            allocateDataSet(
                wsName, dsName, datasetOrganization, "TRK", 10, 1, 1,
                s, recordLength, 3200, 0, remoteRobot, true, ""
            )
            val dsntp = if (datasetOrganization == "PS") {
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
     * Allocates dataset.
     */
    private fun allocateDataSet(
        wsName: String,
        datasetName: String,
        datasetOrganization: String,
        allocationUnit: String,
        primaryAllocation: Int,
        secondaryAllocation: Int,
        directory: Int,
        recordFormat: String,
        recordLength: Int,
        blockSize: Int,
        averageBlockLength: Int,
        remoteRobot: RemoteRobot,
        isValid: Boolean,
        message: String
    ) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, "Dataset").click()
            allocateDatasetDialog(fixtureStack) {
                allocateDataset(
                    datasetName,
                    datasetOrganization,
                    allocationUnit,
                    primaryAllocation,
                    secondaryAllocation,
                    directory,
                    recordFormat,
                    recordLength,
                    blockSize,
                    averageBlockLength
                )
                clickButton("OK")
                Thread.sleep(5000)
            }
            if (isValid) {
                closableFixtureCollector.closeOnceIfExists(AllocateDatasetDialog.name)
                find<ContainerFixture>(byXpath("//div[@class='MyDialog']")).findText("Dataset $datasetName Has Been Created")
                clickButton("No")
            } else {
                val msgAll = find<HeavyWeightWindowFixture>(
                    byXpath("//div[@class='HeavyWeightWindow']"),
                    Duration.ofSeconds(30)
                ).findAllText()
                var msg = ""
                msgAll.forEach { msg += it.text }
                msg.shouldContain(message)
                assertFalse(button("OK").isEnabled())
                clickButton("Cancel")
                closableFixtureCollector.closeOnceIfExists(AllocateDatasetDialog.name)
            }
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
                find<ComponentFixture>(viewTree).findAllText(wsName).last().doubleClick()
                Thread.sleep(10000)
            }
        }
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


    /**
     * Creates working set and z/OS mask.
     */
    private fun createWsAndMask(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWsWithoutMask(projectName, wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair("$datasetName*", "z/OS"))
                Thread.sleep(3000)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
    }
}