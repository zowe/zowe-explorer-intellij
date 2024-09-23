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
import com.intellij.remoterobot.fixtures.ActionButtonFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.stepsProcessing.step
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.auxiliary.components.dialogs.AddWorkingSetSubDialog
import workingset.auxiliary.components.dialogs.EditWorkingSetSubDialog
import workingset.auxiliary.components.dialogs.SettingsDialogUtil
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.*
import java.time.Duration


/**
 * Tests creating working sets and masks via settings.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetViaSettingsTest : IdeaInteractionClass() {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Settings Dialog", "Add Working Set Dialog", "Edit Working Set Dialog"
    )
    private var longWsName: String = "A".repeat(200)
    private var settingsDialog = SettingsDialogUtil()
    override val connectionName = "valid connection"
    private val newValidConnectionName = "new valid connection"
    private var okButtonSub = ButtonElement()
    private var cancelButtonSub = ButtonElement()
    private var removeMaskButtonSub = ButtonElement()
    private var removeMaskButtonMain = ButtonElement()
    private var removeButton = ButtonElement()
    private val invalidConnectionName = "invalid connection"
    private val invalidConnectionName2 = "invalid connection2"
    private val wsNameAfterRename = "WS11"
    private val alreadyExistsWorkingSetName = "already"
    private val workingSetNameForDelete = "deleteMe"
    private val workingSetNameForDelete2 = "deleteMe2"
    private val workingSetNameForDelete3 = "deleteMe3"
    private lateinit var processManager: ProcessManager


    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        settingsDialog = SettingsDialogUtil(fixtureStack, remoteRobot)
        addWorkingSetDialog = AddWorkingSetSubDialog(fixtureStack, remoteRobot)
        editWorkingSetSubDialog = EditWorkingSetSubDialog(fixtureStack, remoteRobot)
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        okButtonSub = ButtonElement(subOkButtonLoc, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
        cancelButtonSub = ButtonElement(subCancelButtonLoc, fixtureStack, remoteRobot)
        removeButton = ButtonElement(removeButtonLoc, fixtureStack, remoteRobot)
        removeMaskButtonSub = ButtonElement(removeButtonSubLoc, fixtureStack, remoteRobot)
        removeMaskButtonMain = ButtonElement(removeButtonLocAnother, fixtureStack, remoteRobot)

        injectTestInfo(testInfo)
        injectTestInfoRestTopology(testInfo)
        createConnection(
            fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
        )
    }


    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(){
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

    /**
     * Tests to add new working set without connection, checks that correct message is returned.
     */
//    @Disabled("waiting for the fix to be applied https://jira.ibagroup.eu/browse/IJMP-1498")
    @Test
    fun testAddWorkingSetWithoutConnectionViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        step("test") {
            callSettingsByAction(fixtureStack, remoteRobot)
            settingsDialog.workingSetsTab.click()
            settingsDialog.callAddWs(fixtureStack, remoteRobot, closableFixtureCollector)
            addWorkingSetDialog.setWsName(WS_NAME_1, fixtureStack, remoteRobot)
            okButtonSub.click()

            find<HeavyWeightWindowFixture>(messageLoc).findText(MUST_PROVIDE_CONNECTION_MESSAGE)
        }
        step("post action"){
            cancelButtonSub.click()
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)

            settingsDialog.cancelButton.click()
            closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        }
    }

    /**
     * Tests to add new empty working set with very long name, checks that correct message is returned.
     */
    @Test
    @Disabled("decided it wasn't a problem")
    fun testAddEmptyWorkingSetWithVeryLongNameViaSettings(remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            callSettingsByAction(fixtureStack, remoteRobot)
            settingsDialog.workingSetsTab.click()
            settingsDialog.callAddWs(fixtureStack, remoteRobot, closableFixtureCollector)
            addWorkingSetDialog.setWsName(longWsName, fixtureStack, remoteRobot)
            okButtonSub.click()
            find<HeavyWeightWindowFixture>(messageLoc).findText(MUST_PROVIDE_CONNECTION_MESSAGE)
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                settingsDialog(fixtureStack) {
                    closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
            }
        }

    /**
     * Tests to add new working set with one valid mask.
     */
    @Test
    fun testAddWorkingSetWithOneValidMaskViaSettings(remoteRobot: RemoteRobot){
        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.callAddWs(fixtureStack, remoteRobot, closableFixtureCollector)
        addWorkingSetDialog.setWsName(WS_NAME_1, fixtureStack, remoteRobot)
        addWorkingSetDialog.setMaskPair(singleMask, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(addWorkingSetDialog.dialogTitle)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
    }

    /**
     * Tests to add new working set with several valid z/OS masks, opens masks in explorer.
     */
    @Test
    fun testAddWorkingSetWithValidZOSMasksViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot){
        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.callAddWs(fixtureStack, remoteRobot, closableFixtureCollector)
        addWorkingSetDialog.setWsName(WS_NAME_2, fixtureStack, remoteRobot)
        validZOSMasks.forEach {
            addWorkingSetDialog.setMaskPair(Pair(it, ZOS_MASK), fixtureStack, remoteRobot)
        }
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)

        injectListEmptyData(testInfo)
        compressWsIfcDecompressed(WS_NAME_2, validZOSMasks[0], fixtureStack, remoteRobot)
        validZOSMasks.forEach {
            openWSOpenMaskInExplorer(WS_NAME_2, it.uppercase(), fixtureStack, remoteRobot)
        }
    }

    /**
     * Tests to add new working set with several valid USS masks, opens masks in explorer.
     */
    @Test
    fun testAddWorkingSetWithValidUSSMasksViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot){

        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.callAddWs(fixtureStack, remoteRobot, closableFixtureCollector)
        addWorkingSetDialog.setWsName(WS_NAME_3, fixtureStack, remoteRobot)
        validUSSMasks.forEach {
            addWorkingSetDialog.setMaskPair(Pair(it, USS_MASK), fixtureStack, remoteRobot)
        }
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        settingsDialog.okButton.click()

        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        injectListEmptyData(testInfo)
        compressWsIfcDecompressed(WS_NAME_3, validUSSMasks[0], fixtureStack, remoteRobot)
        validUSSMasks.forEach {
            openWSOpenMaskInExplorer(WS_NAME_3, it, fixtureStack, remoteRobot)
        }
    }


    /**
     * Tests to add new working set with invalid masks, checks that correct messages are returned.
     */
    @Test
    fun testAddWorkingSetWithInvalidMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.callAddWs(fixtureStack, remoteRobot, closableFixtureCollector)
        addWorkingSetDialog.setWsName(WS_NAME_4, fixtureStack, remoteRobot)
        maskMessageMap.forEach {
            addWorkingSetDialog.setMaskPair(Pair(it.key, ZOS_MASK), fixtureStack, remoteRobot)
            if (okButtonSub.isEnabled()) {
                okButtonSub.click()
            } else {
                okButtonSub.moveMouse()
            }
            if (it.key.length < 49) {
                addWorkingSetDialog.moveToText(it.key.uppercase(),fixtureStack,remoteRobot)
            } else {
                addWorkingSetDialog.moveToText("${it.key.uppercase().substring(0, 46)}...",fixtureStack,remoteRobot)
            }
            Thread.sleep(3000)
            find<HeavyWeightWindowFixture>(helpLoc).findText(
                it.value
            )
            assertFalse(okButtonSub.isEnabled())
            removeButton.click()
        }
        cancelButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        settingsDialog.okButton.click()

    }

    /**
     * Tests to add working set with the same masks, checks that correct message is returned.
     */
    @Test
    fun testAddWorkingSetWithTheSameMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {

        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.callAddWs(fixtureStack, remoteRobot, closableFixtureCollector)
        addWorkingSetDialog.setWsName(WS_NAME_5, fixtureStack, remoteRobot)
        addWorkingSetDialog.setMaskPair(singleMask, fixtureStack, remoteRobot)
        addWorkingSetDialog.setMaskPair(singleMask, fixtureStack, remoteRobot)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        find<HeavyWeightWindowFixture>(messageLoc).findText(IDENTICAL_MASKS_MESSAGE)
        cancelButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
    }

    /**
     * Tests to edit working set by adding one mask, checks that ws is refreshed in explorer, opens new mask.
     */
    @Test
    fun testEditWorkingSetAddOneMaskViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot){

        injectListEmptyData(testInfo)
        createWsWithConnectionFromAction(connectionName, WS_NAME_5, singleMask, fixtureStack, remoteRobot)
        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.selectWs(WS_NAME_5, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editWorkingSetSubDialog.setMaskPair(singleUssMask, fixtureStack,remoteRobot)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)

        injectListEmptyData(testInfo)
        decompressWsIfCompressed(WS_NAME_5,ussMask,fixtureStack, remoteRobot)
        openMaskInExplorer(ussMask, "", fixtureStack, remoteRobot)
        compressAndDecompressTree(WS_NAME_5, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by deleting several masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    fun testEditWorkingSetDeleteMasksViaSettings(remoteRobot: RemoteRobot){
        createWsWithConnectionFromAction("", WS_NAME_6, validZOSMasks, fixtureStack,remoteRobot)
        val masks = validZOSMasks.slice(0..2)
        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.selectWs(WS_NAME_6, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editWorkingSetSubDialog.deleteMasks(masks, fixtureStack, remoteRobot)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        masks.forEach { checkItemWasDeletedWSRefreshed(it.uppercase(), fixtureStack, remoteRobot) }
    }

    /**
     * Tests to edit working set by deleting all masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Disabled("waiting for the fix to be applied https://jira.ibagroup.eu/browse/IJMP-1498")
    @Test
    fun testEditWorkingSetDeleteAllMasksViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectTestInfo(testInfo)
        injectTestInfoRestTopology(testInfo)
        createConnection(
            fixtureStack,closableFixtureCollector,connectionName, true,remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
        )
        createWsWithConnectionFromAction("", WS_NAME_6, validZOSMasks, fixtureStack,remoteRobot)
        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.selectWs(WS_NAME_6, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editWorkingSetSubDialog.deleteMasks(validZOSMasks, fixtureStack, remoteRobot)
        okButtonSub.click()
        find<HeavyWeightWindowFixture>(messageLoc).findText(EMPTY_DATASET_MESSAGE)
        okButtonSub.click()

        validZOSMasks.forEach { checkItemWasDeletedWSRefreshed(it.uppercase(), fixtureStack, remoteRobot) }
        openOrCloseWorkingSetInExplorer(wsName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by changing connection to invalid, checks that correct message is returned.
     */
    @Test
    fun testEditWorkingSetChangeConnectionToInvalidViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            injectInvalidUrlPortInfo(testInfo, PORT_104431)
            injectEmptyZosmfRestfilesPath(testInfo)
            createConnection(fixtureStack, closableFixtureCollector, invalidConnectionName, false, remoteRobot,
                "https://${mockServer.hostName}:$PORT_10443"
            )
            createWsWithConnectionFromAction("", WS_NAME_7, validZOSMasks, fixtureStack,remoteRobot)

            callSettingsByAction(fixtureStack, remoteRobot)
            settingsDialog.workingSetsTab.click()
            settingsDialog.selectWs(WS_NAME_7, fixtureStack, remoteRobot)
            settingsDialog.editWsButton.click()
            editWorkingSetSubDialog.setConnection(invalidConnectionName, fixtureStack,remoteRobot)
            okButtonSub.click()
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
            okButton.click()
            closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
            findAll<ComponentFixture>(invalidPortInTreesLoc).forEach {
                it.click()
                findAll<ActionButtonFixture>(errorCloseIconLoc).first().click()
            }
            openMaskInExplorer(
                zosUserDatasetMask.uppercase(),
                invalidPort104431,
                fixtureStack,
                remoteRobot
            )
            compressAndDecompressTree(WS_NAME_7, fixtureStack, remoteRobot)
        }

    /**
     * Tests to edit working set by changing connection from invalid to valid, checks that ws is refreshed in explorer and error message disappeared.
     */
    @Test
    fun testEditWorkingSetChangeConnectionToValidViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot){
            injectTestInfo(testInfo)
            injectTestInfoRestTopology(testInfo)
            createConnection(fixtureStack,closableFixtureCollector,newValidConnectionName, true, remoteRobot, "https://${mockServer.hostName}:${mockServer.port}")
            createConnection(fixtureStack,closableFixtureCollector,invalidConnectionName2, false, remoteRobot, "https://${mockServer.hostName}:${mockServer.port}")

            createWsWithConnectionFromAction(invalidConnectionName2, WS_NAME_9, validZOSMasks, fixtureStack,remoteRobot)
            decompressWsIfCompressed(WS_NAME_9,validZOSMasks[0], fixtureStack, remoteRobot)
            injectEmptyZosmfRestfilesPath(testInfo)
            callSettingsByAction(fixtureStack, remoteRobot)
            settingsDialog.workingSetsTab.click()
            settingsDialog.selectWs(WS_NAME_9, fixtureStack, remoteRobot)
            settingsDialog.editWsButton.click()
            editWorkingSetSubDialog.setConnection(newValidConnectionName, fixtureStack,remoteRobot)
            okButtonSub.click()
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
            okButton.click()
            checkItemWasDeletedWSRefreshed(invalidPort104431, fixtureStack, remoteRobot)
            openOrCloseWorkingSetInExplorer(WS_NAME_9, fixtureStack, remoteRobot)
        }

    /**
     * Tests to edit working set by renaming it, checks that ws is refreshed in explorer.
     */
    @Test
    fun testEditWorkingSetRenameViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {

        createWsWithoutMask(WS_NAME_10, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        createWsWithoutMask(alreadyExistsWorkingSetName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)

        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.selectWs(WS_NAME_10, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editWorkingSetSubDialog.renameWorkingSet(alreadyExistsWorkingSetName)
        okButtonSub.click()
        val message = find<HeavyWeightWindowFixture>(messageLoc, Duration.ofSeconds(30)).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_WORKING_SET_NAME.format(alreadyExistsWorkingSetName))
        editWorkingSetSubDialog.renameWorkingSet(wsNameAfterRename)

        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)

        checkItemWasDeletedWSRefreshed(WS_NAME_10, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsNameAfterRename, fixtureStack, remoteRobot)

    }

    /**
     * Tests to delete working set, checks that explorer info is refreshed.
     */
    @Test
    fun testDeleteWorkingSetViaSettings(remoteRobot: RemoteRobot){
        createWsWithoutMask(workingSetNameForDelete, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)

        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.selectWs(workingSetNameForDelete, fixtureStack, remoteRobot)
        removeMaskButtonMain.click()
        okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        checkItemWasDeletedWSRefreshed(workingSetNameForDelete, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete all working sets, checks that explorer info is refreshed.
     */
    @Test
    fun testDeleteAllWorkingSetsViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWsWithoutMask(workingSetNameForDelete2, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        createWsWithoutMask(workingSetNameForDelete3, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        callSettingsByAction(fixtureStack, remoteRobot)
        settingsDialog.workingSetsTab.click()
        settingsDialog.deleteAllMask(fixtureStack,remoteRobot)
        okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        assertTrue(find<ComponentFixture>(viewTree).findText(NOTHING_TO_SHOW_MSG).text==NOTHING_TO_SHOW_MSG)
    }

}
