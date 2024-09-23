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
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import testutils.ProcessManager
import workingset.testutils.injectListAllAllocatedDatasets


/**
 * Tests allocating datasets with valid and invalid inputs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class DeleteDatasetTest : IdeaInteractionClass() {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(ALLOCATE_DATASET_DIALOG)
    override val datasetName = "$ZOS_USERID.ALLOC."
    private val recordFormats = mutableListOf(F_RECORD_FORMAT_SHORT, FB_RECORD_FORMAT_SHORT, V_RECORD_FORMAT_SHORT, VA_RECORD_FORMAT_SHORT, VB_RECORD_FORMAT_SHORT)
    private var datasetsToBeDeleted = mutableListOf<String>()
    private var mapListDatasets = mutableMapOf<String, String>()
    private lateinit var processManager: ProcessManager

    /**
     * Opens the project and Explorer, clears test environment, creates working set and mask.
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
        createWsAndMask(remoteRobot)
    }

    /**
     * Closes the project and clears test environment, deletes created datasets.
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

    //todo add tests with advanced parameters when switched to mock

    /**
     * Tests to allocate PO datasets with valid parameters.
     */
    @ParameterizedTest
    @ValueSource(strings = [SEQUENTIAL_ORG_FULL, PO_ORG_FULL, POE_ORG_FULL])
    fun testDeleteDatasets(input: String, remoteRobot: RemoteRobot) {
        doValidTest(input, remoteRobot)
        deleteDatasets(remoteRobot)
    }

    /**
     * Allocates dataset with different record formats.
     */
    private fun doValidTest(datasetOrganization: String, remoteRobot: RemoteRobot) {
        recordFormats.forEach { s ->
            val recordLength = if (s == F_RECORD_FORMAT_SHORT) {
                3200
            } else {
                80
            }
            val dsOrganisationShort = "\\((.*?)\\)".toRegex().find(datasetOrganization)?.groupValues?.get(1)
            val dsName = "${datasetName}${dsOrganisationShort}.${s}".uppercase().replace("-", "")

            if (dsOrganisationShort != null) {
                responseDispatcher.injectAllocationResultPo(
                    datasetOrganization,
                    s,
                    dsName,
                    dsOrganisationShort,
                    recordLength
                )
            }
            allocateDataSet(
                wsName, dsName, datasetOrganization, TRACKS_ALLOCATION_UNIT_SHORT, 10, 1, 1,
                s, recordLength, 3200, 0, remoteRobot
            )

            val dsntp = if (dsOrganisationShort == SEQUENTIAL_ORG_SHORT) {
                ""
            } else {
                PDS_TYPE
            }

            val listDs = buildDatasetConfigString(dsName, dsntp, datasetOrganization, recordLength, s)
            mapListDatasets[dsName] = listDs
            datasetsToBeDeleted.add(dsName)
        }
    }

    /**
     * Deletes created datasets.
     */
    private fun deleteDatasets(remoteRobot: RemoteRobot) {
        injectListAllAllocatedDatasets(datasetName.uppercase(), mapListDatasets)
        responseDispatcher.injectListMembers(NO_MEMBERS)

        refreshWorkSpace(wsName, fixtureStack, remoteRobot)
        compressAndDecompressTree(wsName, fixtureStack, remoteRobot)

        datasetsToBeDeleted.forEach { s ->
            mapListDatasets.remove(s)
            responseDispatcher.injectAllocatedDatasets(s, buildFinalListDatasetJson(mapListDatasets), s)
            responseDispatcher.injectDeleteDataset(s)
            deleteDataset(s, fixtureStack, remoteRobot)
        }
        compressAndDecompressTree(wsName, fixtureStack, remoteRobot)

        datasetsToBeDeleted.clear()
    }
}
