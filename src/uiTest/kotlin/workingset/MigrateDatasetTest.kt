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

package workingset

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.testutils.injectListAllAllocatedDatasets

/**
 * Tests migrating and recalling dataset.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class MigrateDatasetTest:IdeaInteractionClass() {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()

    override val connectionName = "con1"
    override val wsName = "WS name"
    private val maskName = "${ZOS_USERID.uppercase()}.UI.TEST*"

    private val pdsName = "${ZOS_USERID.uppercase()}.UI.TEST"
    private var mapListDatasets = mutableMapOf<String, String>()
    private val mapListDatasetMembers = mutableMapOf<String, String>()

    private val migratedDs = "{\"dsname\": \"$pdsName\",\"migr\": \"YES\",\"vol\": \"MIGRAT\"},"
    private lateinit var processManager: ProcessManager

    /**
     * Opens the project and Explorer, clears test environment, creates working set, mask and pds.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo,remoteRobot: RemoteRobot) = with(remoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        responseDispatcher.injectTestInfo(testInfo)
        responseDispatcher.injectTestInfoRestTopology(testInfo)
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(
            fixtureStack,
            closableFixtureCollector,
            connectionName,
            true,
            remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
        )
        mapListDatasets[pdsName] = auxiliary.listDS(pdsName, PDS_TYPE, PO_ORG_SHORT)
        injectListAllAllocatedDatasets(pdsName, mapListDatasets, true)
        responseDispatcher.injectListAllDatasetMembersRestfiles(
            pdsName,
            buildResponseListJson(mapListDatasetMembers, false)
        )
        responseDispatcher.injectAllocationResultPds(pdsName)

        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)

        allocatePDSAndCreateMask(
            wsName, pdsName, fixtureStack, closableFixtureCollector, remoteRobot, maskName, directory = 2
        )
    }


    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        processManager.close()
        mockServer.shutdown()
    }

    /**
     * Test to migrate and recall dataset
     */
    @Test
    fun testViewJobProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            migrateDataset(pdsName, migratedDs, mapListDatasets, fixtureStack, remoteRobot)
            recallDataset(pdsName, fixtureStack, mapListDatasets, remoteRobot)
        }
    }
}
