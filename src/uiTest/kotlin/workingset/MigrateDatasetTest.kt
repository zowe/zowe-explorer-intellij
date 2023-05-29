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
 * Tests migrating and recalling dataset.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class MigrateDatasetTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()

    private val projectName = "untitled"
    private val connectionName = "con1"
    private val wsName = "WS name"
    private val maskName = "${ZOS_USERID.uppercase()}.UI.TEST*"

    private val pdsName = "${ZOS_USERID.uppercase()}.UI.TEST"
    private var mapListDatasets = mutableMapOf<String, String>()
    private val mapListDatasetMembers = mutableMapOf<String, String>()

    private val migratedDs = "{\"dsname\": \"$pdsName\",\"migr\": \"YES\",\"vol\": \"MIGRAT\"},"

    /**
     * Opens the project and Explorer, clears test environment, creates working set, mask and pds.
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
        responseDispatcher.injectEndpoint(
            "listAllAllocatedDatasets_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${maskName}") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListDatasets, true)) }
        )
        responseDispatcher.injectEndpoint(
            "listAllDatasetMembers_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/$pdsName/member") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListDatasetMembers, false)) }
        )
        responseDispatcher.injectEndpoint(
            "allocatePds_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/$pdsName") ?: false },
            { MockResponse().setBody("{\"dsorg\":\"PDS\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
        )
        createWsWithoutMask(projectName, wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        mapListDatasets[pdsName] = listDS(pdsName, "PDS", "PO")
        allocatePDSAndCreateMask(
            wsName,
            pdsName,
            projectName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot,
            maskName,
            directory = 2
        )
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        mockServer.shutdown()
        clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            close()
        }
    }

    /**
     * Test to migrate and recall dataset
     */
    @Test
    @Order(1)
    fun testViewJobProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(pdsName).rightClick()
            }
            responseDispatcher.injectEndpoint(
                "migratePds_restfiles",
                { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/$pdsName") ?: false &&
                        it?.body?.toString()?.contains("hmigrate") ?: false },
                { MockResponse().setResponseCode(200) }
            )
            mapListDatasets[pdsName] = migratedDs
            actionMenuItem(remoteRobot, "Migrate").click()
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(pdsName).rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            datasetPropertiesDialog(fixtureStack) {
                if (isDatasetMigrated()) {
                    throw Exception("Dataset is not migrated")
                }
                clickButton("OK")
            }
            responseDispatcher.injectEndpoint(
                "migratePds_restfiles",
                { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/$pdsName") ?: false &&
                        it?.body?.toString()?.contains("hrecall") ?: false },
                { MockResponse().setResponseCode(200) }
            )
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(pdsName).rightClick()
            }
            mapListDatasets[pdsName] = listDS(pdsName, "PDS", "PO")
            actionMenuItem(remoteRobot, "Recall").click()
        }
    }
}