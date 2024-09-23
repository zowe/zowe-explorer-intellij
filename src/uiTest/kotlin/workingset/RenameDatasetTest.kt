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
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import testutils.ProcessManager
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.*
import java.time.Duration
import java.util.stream.Stream

/**
 * Tests creating, editing and deleting working sets and masks from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class RenameDatasetTest : IdeaInteractionClass() {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Allocate DataSet Dialog", "Allocate Member Dialog")
    override val connectionName = "con1"
    override val wsName = "WS name"


    private val pdsName = "$ZOS_USERID.UI.TEST".uppercase()
    private val pdsMaskName = "$ZOS_USERID.UI.TEST*".uppercase()
    private val memberName = "TESTM"
    private val memberFinalName = "TESTMF"
    private val anotherMemberName = "TESTMA"

    private val dsName =        "$ZOS_USERID.UI.TESTD".uppercase()
    private val dsFinalName =   "$ZOS_USERID.UI.TESTDF".uppercase()
    private val anotherDsName = "$ZOS_USERID.UI.TESTA".uppercase()
    private var mapListDatasets = mutableMapOf<String, String>()
    private val dsFinalNameLong =   "$ZOS_USERID.UI.TESTDF.123456789".uppercase()
    private val tooLongString45 = "A".repeat(45)
    private lateinit var processManager: ProcessManager


    companion object {
        @JvmStatic
        fun valuesProviderMembers(): Stream<Arguments> {
            return incorrectRenameMember.entries.stream().map { entry ->
                Arguments.of(entry.key, entry.value)
            }
        }
    }

    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(testInfo, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        createMask(wsName, pdsMaskName, fixtureStack, closableFixtureCollector, ZOS_MASK, remoteRobot)

        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
    }

    @BeforeEach
    fun setUp(testInfo: TestInfo, remoteRobot: RemoteRobot){
        injectTestInfoForPdsDataset(testInfo.displayName, DATASET_FOR_RENAME_PROPERTY, pdsName)

        responseDispatcher.injectAllocationResultPo(PO_ORG_FULL, VB_RECORD_FORMAT_SHORT, dsName, PO_ORG_SHORT, 255)
        responseDispatcher.injectAllocationResultPo(PO_ORG_FULL, VB_RECORD_FORMAT_SHORT, anotherDsName, PO_ORG_SHORT, 255)

        mapListDatasets[dsName] = listDS(dsName, PDS_TYPE, PO_ORG_SHORT)
        mapListDatasets[anotherDsName] = listDS(anotherDsName, PDS_TYPE, PO_ORG_SHORT)
        mapListDatasets[pdsName] = listDS(pdsName, PDS_TYPE, PO_ORG_SHORT)

        openWSAndListDatasets(testInfo, remoteRobot)
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
        compressAndDecompressTree(wsName, fixtureStack, remoteRobot)
        mapListDatasets.clear()
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests renaming member when valid member name is provided.
     */
    @Test
    fun testRenameMemberWithCorrectNameViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        var isFirstRequest = true
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_listmembers1",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${pdsName}/member") ?: false && isFirstRequest },
            { MockResponse().setBody("{\"items\":[{\"member\": \"${memberName}\"},{\"member\": \"${anotherMemberName}\"}],\"returnedRows\": 2,\"JSONversion\": 1}") }
        )
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_listmembers2",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${pdsName}/member") ?: false && !isFirstRequest },
            { MockResponse().setBody("{\"items\":[{\"member\": \"${memberFinalName}\"},{\"member\": \"${anotherMemberName}\"}],\"returnedRows\": 2,\"JSONversion\": 1}") }
        )
        injectRenameMember(testInfo,pdsName,memberFinalName,memberName)
        callRenameMemberPoint(fixtureStack,pdsName,memberName, remoteRobot)
        newMemberNameInput(memberFinalName, fixtureStack, remoteRobot)
        isFirstRequest = false
        return clickByText(OK_TEXT, fixtureStack, remoteRobot)
    }

    // todo: 3 tests with the same scenario, create 1 test with parametrisation
    /**
     * Tests renaming member to name of another member in the same PDS and validates error pop-up notification.
     */
    @Test
    fun testRenameMemberWithNameOfAnotherMemberViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        injectRenameMemberUnsuccessful(testInfo, pdsName, memberFinalName, anotherMemberName, 500, "4.0", MEMBER_ALREADY_EXISTS)
        injectMemberList(testInfo, pdsName, listOf(memberFinalName, anotherMemberName))
        callRenameMemberPoint(fixtureStack,pdsName,memberFinalName, remoteRobot)
        newMemberNameInput(anotherMemberName, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        checkErrorNotification(RENAME_MEMBER_FAILED, RENAME_MEMBER_FAILED, MEMBER_ALREADY_EXISTS, fixtureStack, remoteRobot)
        closeNotificztion(fixtureStack, remoteRobot)
    }

    /**
     * Tests renaming DataSet to name of another DataSet and validates error pop-up notification.
     */
    @Test
    fun testRenameDatasetWithNameOfAnotherDatasetViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        injectRenameDatasetUnsuccessful(testInfo, dsFinalName, anotherDsName, 500, RC_8, RC_8_TEXT)
        injectListAllAllocatedDatasets("$ZOS_USERID.UI.TEST*".uppercase(),mapListDatasets)
        callRenameDatasetPoint(fixtureStack, dsName, remoteRobot)
        newDatasetNameInput(anotherDsName,fixtureStack,remoteRobot)
        clickByText(OK_TEXT,fixtureStack,remoteRobot)
        checkErrorNotification(DATA_SET_RENAME_FAILED_MSG, DATA_SET_RENAME_FAILED_MSG, DATA_SET_RENAME_FAILED, fixtureStack, remoteRobot)
        closeNotificztion(fixtureStack, remoteRobot)
    }

    /**
     * Tests renaming member to the same name and validates error pop-up notification.
     */
    @Test
    fun testRenameMemberWithTheSameNameViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot)  {
        injectRenameMemberUnsuccessful(testInfo, pdsName, memberFinalName, memberFinalName, 500, "12.0", MEMBER_IN_USE)
        injectMemberList(testInfo, pdsName, listOf(memberFinalName, anotherMemberName))
        callRenameMemberPoint(fixtureStack,pdsName,memberFinalName, remoteRobot)
        newMemberNameInput(memberFinalName, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).hasText(DUBLICETA_MEMBER_NAME_ERROR)
        cancelButton.click()
    }

    /**
     * Tests renaming member to the incorrect name and validates error pop-up notification.
     */
    @ParameterizedTest
    @MethodSource("valuesProviderMembers")
    fun testIncorrectRename(invalidName: String, errorMsg: String, testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectRenameMemberUnsuccessful(testInfo, pdsName, memberFinalName, memberFinalName, 500, "12.0", MEMBER_IN_USE)
        injectMemberList(testInfo, pdsName, listOf(memberFinalName, anotherMemberName))
        callRenameMemberPoint(fixtureStack,pdsName,memberFinalName,remoteRobot)
        newMemberNameInput(invalidName, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)


        val msgAll = find<HeavyWeightWindowFixture>(messageLoc, Duration.ofSeconds(30)
        ).findAllText()
        var msg = ""
        msgAll.forEach { msg += it.text }

        clickByText(CANCEL_TEXT, fixtureStack, remoteRobot)
        Assertions.assertEquals(errorMsg, msg)
    }

    /**
     * Tests renaming DataSet when valid member name is provided.
     */
    @Test
    fun testRenameDataSetWithCorrectNameViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        injectRenameMember(testInfo, pdsName, memberFinalName, anotherMemberName)
        injectMemberList(testInfo, pdsName, listOf(memberFinalName, anotherMemberName))
        injectRenameDataset(testInfo, dsFinalName, dsName)
        mapListDatasets.remove(dsName)
        mapListDatasets[dsFinalName] = listDS(dsFinalName, PDS_TYPE, PO_ORG_SHORT)
        injectListAllAllocatedDatasets("$ZOS_USERID.UI.TEST*".uppercase(),mapListDatasets)
        callRenameDatasetPoint(fixtureStack, dsName, remoteRobot)
        newDatasetNameInput(dsFinalName,fixtureStack,remoteRobot)
        okButton.click()
    }

    /**
     * Tests renaming DataSet to name with invalid section and validates error notification.
     */
    @Test
    fun testRenameDatasetWithInvalidSectionViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callRenameDatasetPoint(fixtureStack, anotherDsName, remoteRobot)
        newDatasetNameInput(dsFinalNameLong,fixtureStack,remoteRobot)
        clickByText(OK_TEXT,fixtureStack,remoteRobot)

        val msgAll = find<HeavyWeightWindowFixture>(messageLoc, Duration.ofSeconds(30)
        ).findAllText()
        var msg = ""
        msgAll.forEach { msg += it.text }

        clickByText(CANCEL_TEXT, fixtureStack, remoteRobot)
        Assertions.assertEquals(DATASET_INVALID_SECTION_MESSAGE, msg)
    }

    /**
     * Tests renaming DataSet to very long name and validates error notification.
     */
    @Test
    fun testRenameDatasetWithTooLongNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callRenameDatasetPoint(fixtureStack, anotherDsName, remoteRobot)
        newDatasetNameInput(tooLongString45,fixtureStack,remoteRobot)
        clickByText(OK_TEXT,fixtureStack,remoteRobot)

        val msgAll = find<HeavyWeightWindowFixture>(messageLoc, Duration.ofSeconds(30)
        ).findAllText()
        var msg = ""
        msgAll.forEach { msg += it.text }

        clickByText(CANCEL_TEXT, fixtureStack, remoteRobot)
        Assertions.assertEquals(DATASET_NAME_LENGTH_MESSAGE, msg)
    }

    /**
     * Tests renaming DataSet to empty name and validates error notification.
     */
    @Test
    fun testRenameDatasetWithEmptyNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callRenameDatasetPoint(fixtureStack, anotherDsName, remoteRobot)
        newDatasetNameInput(EMPTY_STRING,fixtureStack,remoteRobot)
        clickByText(OK_TEXT,fixtureStack,remoteRobot)

        val msgAll = find<HeavyWeightWindowFixture>(messageLoc, Duration.ofSeconds(30)
        ).findAllText()
        var msg = ""
        msgAll.forEach { msg += it.text }

        clickByText(CANCEL_TEXT, fixtureStack, remoteRobot)
        Assertions.assertEquals(MEMBER_EMPTY_NAME_MESSAGE, msg)
    }

    private fun openWSAndListDatasets(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles",
            {
                it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=$pdsMaskName")
                    ?: false
            },
            { MockResponse().setBody(buildFinalListDatasetJson()) }
        )
        openOrCloseWorkingSetInExplorer(wsName, fixtureStack, remoteRobot)
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
}
