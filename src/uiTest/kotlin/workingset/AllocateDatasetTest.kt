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
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import testutils.ProcessManager
import java.time.Duration
import java.util.stream.Stream

/**
 * Tests allocating datasets with valid and invalid inputs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class AllocateDatasetTest : IdeaInteractionClass() {

    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog"
    )

    override val datasetName = "$ZOS_USERID.ALLOC."
    override val wsName = "WS1"
    private val datasetMask = "$ZOS_USERID.ALLOC."
    private val recordFormats = mutableListOf("F", "FB", "V", "VA", "VB")
    private var datasetsToBeDeleted = mutableListOf<String>()
    private lateinit var processManager: ProcessManager

    companion object {
        @JvmStatic
        fun valuesProvider(): Stream<Arguments> {
            return invalidAllocateScenarios.entries.stream().map { entry ->
                Arguments.of(entry.key, entry.value)
            }
        }
    }

    private var mapListDatasets = mutableMapOf<String, String>()

    @BeforeAll
    fun setUpAll(testInfo: TestInfo) {
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
        return createWsAndMask(remoteRobot)
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
    fun tearDown() {
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    //todo add tests with advanced parameters when switched to mock

    /**
     * Tests to allocate PO, PS, POE datasets with valid parameters.
     */
    @ParameterizedTest
    @ValueSource(strings = [SEQUENTIAL_ORG_FULL, PO_ORG_FULL, POE_ORG_FULL])
    fun testAllocateValidPODatasets(input: String, remoteRobot: RemoteRobot) {
        doValidTest(input, remoteRobot)
    }

    /**
     * Tests to allocate dataset with invalid dataset params, checks that correct message is returned.
     */
    @ParameterizedTest
    @MethodSource("valuesProvider")
    fun testInvalidDatasetName(scenarioName: String, value: InvalidAllocate, remoteRobot: RemoteRobot) =
        invalidAllocateDataSet(
            wsName,
            value.datasetName,
            value.datasetOrganization,
            value.allocationUnit,
            value.primaryAllocation,
            value.secondaryAllocation,
            value.directory,
            value.recordFormat,
            value.recordLength,
            value.blockSize,
            value.averageBlockLength,
            remoteRobot,
            value.message
        )

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
            val dsName = "${datasetMask}${dsOrganisationShort}.${s}".uppercase().replace("-", "")

            if (dsOrganisationShort != null) {
                responseDispatcher.injectAllocationResultPo(
                    datasetOrganization, s, dsName, dsOrganisationShort, recordLength
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
     * Allocates dataset.
     */
    private fun invalidAllocateDataSet(
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
        message: String,
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenu(remoteRobot, NEW_POINT_TEXT).click()
            actionMenuItem(remoteRobot, DATASET_POINT_TEXT).click()
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
                clickButton(OK_TEXT)
            }

            val msgAll = find<HeavyWeightWindowFixture>(
                messageLoc,
                Duration.ofSeconds(30)
            ).findAllText()
            var msg = ""
            msgAll.forEach { msg += it.text }
            msg.shouldContain(message)
            assertFalse(button(OK_TEXT).isEnabled())
            clickButton(CANCEL_TEXT)
            closableFixtureCollector.closeOnceIfExists(AllocateDatasetDialog.name)

        }
    }
}
