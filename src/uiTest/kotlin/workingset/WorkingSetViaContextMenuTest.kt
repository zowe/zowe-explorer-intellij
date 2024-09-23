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
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.auxiliary.components.dialogs.*
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.*
import java.time.Duration

/**
 * Tests creating, editing and deleting working sets and masks from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetViaContextMenuTest : IdeaInteractionClass()  {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(EDIT_WORKING_SET, CREATE_MASK_DIALOG, ADD_WORKING_SET_DIALOG)

    override val connectionName: String = "valid connection"
    private val wsNameA: String = "A".repeat(200)
    private val newInvalidName: String = "invalid connection"

    private val newWorkingSetName = "new ws name"
    private val oldWorkingSetName = "old name"
    private val alreadyExistsWorkingSetName = "already exists"
    private val alreadyExistsWorkingSetNameRename = "already exists for rename"
    private val wsForDelete = "ws for delete"
    private val wsForInvalidMask = "ws for invalid mask"
    private val wsWithUssAndDataset = "ws with uss and dataset"
    private val wsWithAlreadyExistsMask = "ws with already exists mask"
    private val wsWithMaskForRename = "ws with mask for rename"
    private val wsWithMaskForDelete = "ws with mask for delete"
    private lateinit var processManager: ProcessManager


    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot, testInfo:TestInfo) {
        processManager = ProcessManager()
        addWorkingSetDialog = AddWorkingSetSubDialog(fixtureStack, remoteRobot)
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(testInfo, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)

        editWorkingSetSubDialog = EditWorkingSetSubDialog(fixtureStack=fixtureStack, remoteRobot=remoteRobot)
        createMaskSubDialog = CreateMaskSubDialog(fixtureStack=fixtureStack, remoteRobot=remoteRobot)
        renameDatasetMaskDialog = RenameDatasetMaskDialog(fixtureStack=fixtureStack, remoteRobot=remoteRobot)
        deletionOfUssPathRoot = DeletionOfUssPathRoot(fixtureStack=fixtureStack, remoteRobot=remoteRobot)
        deletionOfDSMask = DeletionOfDSMask(fixtureStack=fixtureStack, remoteRobot=remoteRobot)

        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
        yesButton = ButtonElement(YES_TEXT, fixtureStack, remoteRobot)
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
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests to add new empty working set with very long name, checks that correct message is returned.
     */
    @Test
    @Disabled("https://jira.ibagroup.eu/browse/IJMP-977")
    fun testAddEmptyWorkingSetWithVeryLongNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsNameA, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        find<HeavyWeightWindowFixture>(messageLoc).findText(EMPTY_DATASET_MESSAGE)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)

    }

    /**
     * Tests to add new working set with one valid mask.
     */
    @Test
    fun testAddWorkingSetWithOneValidMaskViaContextMenu(remoteRobot: RemoteRobot) {
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_1, singleMask, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        return deleteWSFromContextMenu(WS_NAME_1, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new working set with several valid z/OS masks, opens masks in explorer.
     */
    @Test
    fun testAddWorkingSetWithValidZOSMasksViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        //todo allocate dataset with 44 length when 'Allocate Dataset Dialog' implemented
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        validZOSMasks.forEach {masks.add(Pair(it, ZOS_MASK))}
        injectListEmptyData(testInfo)
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_2, masks, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        compressWsIfcDecompressed(WS_NAME_2, validZOSMasks[0],fixtureStack,remoteRobot)
        validZOSMasks.forEach {
            openWSOpenMaskInExplorer(
                WS_NAME_2,
                it.uppercase(),
                fixtureStack,
                remoteRobot
            )
        }
        return deleteWSFromContextMenu(WS_NAME_2, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new working set with several valid USS masks, opens masks in explorer.
     */
    @Test
    fun testAddWorkingSetWithValidUSSMasksViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        validUSSMasks.forEach {
            masks.add(Pair(it, USS_MASK))
        }
        injectListEmptyData(testInfo, false)
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_3, masks, fixtureStack, remoteRobot)
        addWorkingSetDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        compressAndDecompressTree(WS_NAME_3, fixtureStack, remoteRobot)
        validUSSMasks.forEach { openWSOpenMaskInExplorer(WS_NAME_3, it, fixtureStack, remoteRobot) }
        deleteWSFromContextMenu(WS_NAME_3, fixtureStack, remoteRobot)
        }


    /**
     * Tests to add new working set with invalid masks, checks that correct messages are returned.
     */
    @Test
    fun testAddWorkingSetWithInvalidMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        maskMessageMap.forEach {
            val mask = Pair(it.key, ZOS_MASK)
            addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_4, mask, fixtureStack, remoteRobot)
            if(addWorkingSetDialog.okButton.isEnabled()){
                addWorkingSetDialog.okButton.click()
            } else {
                hoverToByTextAddWorkingSet(OK_TEXT, fixtureStack, remoteRobot)
            }
            if (it.key.length < 49) {
                hoverToByTextAddWorkingSet(it.key.uppercase(), fixtureStack, remoteRobot)
            } else {
                hoverToByTextAddWorkingSet("${it.key.uppercase().substring(0, 46)}...", fixtureStack, remoteRobot)
            }
            Thread.sleep(1000)
            find<HeavyWeightWindowFixture>(helpLoc).findText(it.value)
            assertFalse(addWorkingSetDialog.okButton.isEnabled())
            clickToByTextAddWorkingSet(it.key.uppercase(), fixtureStack,remoteRobot)
            Thread.sleep(3000)
            clickActionButtonByXpath(removeButtonLoc, fixtureStack, remoteRobot)
        }
        cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }

    /**
     * Tests to add working set with the same masks, checks that correct message is returned.
     */
    @Test
    fun testAddWorkingSetWithTheSameMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_5, singleMask, fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_5, singleMask, fixtureStack, remoteRobot)
        addWorkingSetDialog.okButton.click()
        Thread.sleep(2000)
        find<HeavyWeightWindowFixture>(messageLoc).findText(IDENTICAL_MASKS_MESSAGE)
        assertFalse(isButtonEnableByTextAddWorkingSet(OK_TEXT, fixtureStack, remoteRobot))
        addWorkingSetDialog.cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }

    /**
     * Tests to edit working set by adding one mask, checks that ws is refreshed in explorer, opens new mask.
     */
    @Test
    fun testEditWorkingSetAddOneMaskViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        injectListEmptyData(testInfo)
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_6, singleMask, fixtureStack, remoteRobot)
        okButton.click()
        callEditWSFromContextMenu(WS_NAME_6, fixtureStack, remoteRobot)
        fillEditWorkingSet(connectionName,WS_NAME_6,singleUssMask,fixtureStack, remoteRobot)
        okButton.click()
        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        decompressWsIfCompressed(WS_NAME_6, ussMask, fixtureStack, remoteRobot)
        openMaskInExplorer(ussMask,"", fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(WS_NAME_6, fixtureStack, remoteRobot)
        return deleteWSFromContextMenu(WS_NAME_6, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by deleting several masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    fun testEditWorkingSetDeleteMasksViaContextMenu(remoteRobot: RemoteRobot) {
        val masks = listOf(zosUserDatasetMask, "Q.*", ZOS_USERID.uppercase())
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        masks.forEach {addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_7, Pair(it, ZOS_MASK), fixtureStack, remoteRobot)}
        okButton.click()
        callEditWSFromContextMenu(WS_NAME_7, fixtureStack, remoteRobot)
        deleteInEditWorkingSet(masks, fixtureStack, remoteRobot)
        okButton.click()
        okButton.click()

        masks.forEach { checkItemWasDeletedWSRefreshed(it.uppercase(), fixtureStack, remoteRobot) }
        openOrCloseWorkingSetInExplorer(WS_NAME_7, fixtureStack, remoteRobot)
        deleteWSFromContextMenu(WS_NAME_7, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
    }

    /**
     * Tests to edit working set by deleting all masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    @Disabled("https://jira.ibagroup.eu/browse/IJMP-977")
    fun testEditWorkingSetDeleteAllMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        validZOSMasks.forEach {addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_8, Pair(it, ZOS_MASK), fixtureStack, remoteRobot)}
        addWorkingSetDialog.okButton.click()
        openOrCloseWorkingSetInExplorer(WS_NAME_8, fixtureStack, remoteRobot)
        callEditWSFromContextMenu(WS_NAME_8, fixtureStack, remoteRobot)
        deleteInEditWorkingSet(validZOSMasks, fixtureStack, remoteRobot)
        okButton.click()
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                find<HeavyWeightWindowFixture>(messageLoc).findText(EMPTY_DATASET_MESSAGE)
                okButton.click()
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        validZOSMasks.forEach { checkItemWasDeletedWSRefreshed(it.uppercase(), fixtureStack, remoteRobot) }
        openOrCloseWorkingSetInExplorer(WS_NAME_8, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by changing connection to invalid, checks that correct message is returned.
     */
    @Test
    fun testEditWorkingSetChangeConnectionToInvalidViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        injectInvalidUrlPortInfo(testInfo,PORT_104431_AND_1)
        injectEmptyZosmfRestfilesPath(testInfo)
        createConnection(fixtureStack, closableFixtureCollector, newInvalidName, false, remoteRobot, "https://${mockServer.hostName}:$PORT_104431")

        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        validZOSMasks.forEach {addWorkingSetDialog.fillAddWorkingSet(connectionName, WS_NAME_9, Pair(it, ZOS_MASK), fixtureStack, remoteRobot)}
        addWorkingSetDialog.okButton.click()

        openOrCloseWorkingSetInExplorer(WS_NAME_9, fixtureStack, remoteRobot)
        callEditWSFromContextMenu(WS_NAME_9, fixtureStack, remoteRobot)
        setInComboBox(newInvalidName,fixtureStack,remoteRobot)
        okButton.click()
        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)

        openMaskInExplorer(
            zosUserDatasetMask, INVALID_URL_PORT.format(PORT_104431_AND_1), fixtureStack, remoteRobot
        )
        return deleteWSFromContextMenu(WS_NAME_9, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by changing connection from invalid to valid, checks that ws is refreshed in explorer and error message disappeared.
     */
    @Test
    fun testEditWorkingSetChangeConnectionToValidViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        injectInvalidUrlPortInfo(testInfo,PORT_104431_AND_1)
        injectTestInfoRestTopology(testInfo)
        createConnection(fixtureStack, closableFixtureCollector, WS_NAME_10, false, remoteRobot, "https://${mockServer.hostName}:$PORT_104431")

        callCreateWSFromContextMenu(fixtureStack, remoteRobot)
        validZOSMasks.forEach {addWorkingSetDialog.fillAddWorkingSet(WS_NAME_10, WS_NAME_10, Pair(it, ZOS_MASK), fixtureStack, remoteRobot)}
        addWorkingSetDialog.okButton.click()

        injectEmptyZosmfRestfilesPath(testInfo)
        callEditWSFromContextMenu(WS_NAME_10, fixtureStack, remoteRobot)
        changeConnectionInEditWorkingSet(connectionName, fixtureStack, remoteRobot)
        okButton.click()

        openOrCloseWorkingSetInExplorer(WS_NAME_10, fixtureStack, remoteRobot)
        checkItemWasDeletedWSRefreshed(INVALID_URL_PORT.format(PORT_104431_AND_1), fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        return deleteWSFromContextMenu(WS_NAME_10, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by renaming it, checks that ws is refreshed in explorer.
     */
    @Test
    fun testEditWorkingSetRenameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWsWithConnectionFromAction(connectionName, oldWorkingSetName, singleMask, fixtureStack, remoteRobot)

        createWsWithConnectionFromAction(
            connectionName, alreadyExistsWorkingSetName, singleMask, fixtureStack, remoteRobot)

        callEditWSFromContextMenu(oldWorkingSetName, fixtureStack, remoteRobot)

        editWorkingSetSubDialog.renameWorkingSet(alreadyExistsWorkingSetName)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)

        val message = find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_WORKING_SET_NAME.format(alreadyExistsWorkingSetName))

        editWorkingSetSubDialog.renameWorkingSet(newWorkingSetName)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)

        closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)

        checkItemWasDeletedWSRefreshed(oldWorkingSetName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(newWorkingSetName, fixtureStack, remoteRobot)
        deleteWSFromContextMenu(alreadyExistsWorkingSetName, fixtureStack, remoteRobot)
        deleteWSFromContextMenu(newWorkingSetName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete working set, checks that explorer info is refreshed.
     */
    @Test
    fun testDeleteWorkingSetViaContextMenu(remoteRobot: RemoteRobot) {
        createWsWithConnectionFromAction(connectionName, wsForDelete, fixtureStack, closableFixtureCollector, remoteRobot)

        deleteWSFromContextMenu(wsForDelete, fixtureStack,remoteRobot)

        return checkItemWasDeletedWSRefreshed(wsForDelete, fixtureStack, remoteRobot)
    }

    /**
     * Tests to create invalid masks, checks that correct messages are returned.
     */
    @Test
    fun testCreateInvalidMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWsWithConnectionFromAction(connectionName, wsForInvalidMask, fixtureStack, closableFixtureCollector, remoteRobot)

        callCreateMask(wsForInvalidMask, fixtureStack,remoteRobot)
        maskMessageMap.forEach {
            createMaskSubDialog.setMask(Pair(it.key, ZOS_MASK))
            Thread.sleep(3000)
            if (okButton.isEnabled()) {
                okButton.click()
            }
            find<HeavyWeightWindowFixture>(messageLoc).findText(
                it.value
            )
            assertFalse(okButton.isEnabled())
        }
        cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(createMaskSubDialog.dialogTitle)
        deleteWSFromContextMenu(wsForInvalidMask, fixtureStack, remoteRobot)
    }

    /**
     * Tests to create valid USS and z/OS masks from context menu.
     */
    @Test
    fun testCreateValidMasksViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        createWsWithConnectionFromAction(connectionName, wsWithUssAndDataset, fixtureStack, closableFixtureCollector, remoteRobot)

        injectEmptyZosmfRestfilesPath(testInfo)

        validZOSMasks.forEach {
            createMask(wsWithUssAndDataset, it, fixtureStack,closableFixtureCollector, ZOS_MASK, remoteRobot)
            openWSOpenMaskInExplorer(wsWithUssAndDataset, it.uppercase(), fixtureStack, remoteRobot)
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
        validUSSMasks.forEach {
            createMask(wsWithUssAndDataset, it, fixtureStack,closableFixtureCollector, USS_MASK, remoteRobot)
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
        return deleteWSFromContextMenu(wsWithUssAndDataset, fixtureStack, remoteRobot)
    }

    /**
     * Tests to create already exists mask in working set, checks that correct message is returned.
     */
    @Test
    fun testCreateAlreadyExistsMaskViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWsWithConnectionFromAction(connectionName, wsWithAlreadyExistsMask, fixtureStack, closableFixtureCollector, remoteRobot)
        createMask(wsWithAlreadyExistsMask, zosUserDatasetMask, fixtureStack,closableFixtureCollector, ZOS_MASK, remoteRobot)

        callCreateMask(wsWithAlreadyExistsMask, fixtureStack,remoteRobot)
        createMaskSubDialog.setMask(singleMask)
        okButton.click()
        assertFalse(okButton.isEnabled())

        val message = find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_MASK.format(wsWithAlreadyExistsMask, zosUserDatasetMask))
        cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        deleteWSFromContextMenu(wsWithAlreadyExistsMask, fixtureStack, remoteRobot)

    }

    /**
     * Tests to rename mask, checks that info is refreshed in explorer.
     */
    @Test
    fun testRenameMasksViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        createWsWithConnectionFromAction(connectionName, wsWithMaskForRename, fixtureStack, closableFixtureCollector, remoteRobot)

        createMask(wsWithMaskForRename, zosUserDatasetMask, fixtureStack,closableFixtureCollector, ZOS_MASK, remoteRobot)
        createMask(wsWithMaskForRename, ussMask, fixtureStack,closableFixtureCollector, USS_MASK, remoteRobot)

        injectEmptyZosmfRestfilesPath(testInfo)
        compressAndDecompressTree(wsWithMaskForRename, fixtureStack, remoteRobot)

        callEditWSFromContextMenu(zosUserDatasetMask, fixtureStack, remoteRobot)
        renameDatasetMaskDialog.renameMaskFromContextMenu(zosUserDatasetMaskDoubleStar)
        okButton.click()

        callEditWSFromContextMenu(ussMask, fixtureStack, remoteRobot)
        renameDatasetMaskDialog.renameMaskFromContextMenu(defaultNewUssMask)
        okButton.click()

        refreshWorkSpace(wsWithMaskForRename, fixtureStack,remoteRobot)

        compressAndDecompressTree(zosUserDatasetMaskDoubleStar, fixtureStack, remoteRobot)
        compressAndDecompressTree(defaultNewUssMask, fixtureStack, remoteRobot)

        checkItemWasDeletedWSRefreshed(zosUserDatasetMask, fixtureStack, remoteRobot)
        checkItemWasDeletedWSRefreshed(ussMask, fixtureStack, remoteRobot)

        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        return deleteWSFromContextMenu(wsWithMaskForRename, fixtureStack, remoteRobot)
    }

    /**
     * Tests to rename mask to already exists, checks that correct message is returned.
     */
    @Test
    fun testRenameMaskToAlreadyExistsViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        // before test preparation
        createWsWithConnectionFromAction(connectionName, alreadyExistsWorkingSetNameRename, fixtureStack, closableFixtureCollector, remoteRobot)

        createMask(alreadyExistsWorkingSetNameRename, zosUserDatasetMask, fixtureStack,closableFixtureCollector, ZOS_MASK, remoteRobot)
        createMask(alreadyExistsWorkingSetNameRename, zosUserDatasetMaskDoubleStar, fixtureStack,closableFixtureCollector, ZOS_MASK, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        createMask(alreadyExistsWorkingSetNameRename, ussMask, fixtureStack,closableFixtureCollector, USS_MASK, remoteRobot)
        createMask(alreadyExistsWorkingSetNameRename, defaultNewUssMask, fixtureStack,closableFixtureCollector, USS_MASK, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)

        compressAndDecompressTree(alreadyExistsWorkingSetNameRename, fixtureStack, remoteRobot)

        //test
        callEditWSFromContextMenu(zosUserDatasetMask, fixtureStack, remoteRobot)
        renameDatasetMaskDialog.renameMaskFromContextMenu(zosUserDatasetMaskDoubleStar)
        okButton.click()

        assertFalse(okButton.isEnabled())
        val message = find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_MASK.format(alreadyExistsWorkingSetNameRename, zosUserDatasetMask))
        cancelButton.click()

        callEditWSFromContextMenu(ussMask, fixtureStack, remoteRobot)
        renameDatasetMaskDialog.renameMaskFromContextMenu(defaultNewUssMask)
        okButton.click()

        assertFalse(okButton.isEnabled())
        val messageUss = find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findAllText()
        (messageUss[0].text + messageUss[1].text).shouldContain(UNIQUE_MASK.format(alreadyExistsWorkingSetNameRename, defaultNewUssMask))
        cancelButton.click()

        deleteWSFromContextMenu(alreadyExistsWorkingSetNameRename, fixtureStack, remoteRobot)

    }

    /**
     * Tests to delete masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    fun testDeleteMaskViaContextMenu(remoteRobot: RemoteRobot) {
        // before test preparation
        createWsWithConnectionFromAction(connectionName, wsWithMaskForDelete, fixtureStack, closableFixtureCollector, remoteRobot)

        createMask(wsWithMaskForDelete, zosUserDatasetMask, fixtureStack,closableFixtureCollector, ZOS_MASK, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        createMask(wsWithMaskForDelete, ussMask, fixtureStack,closableFixtureCollector, USS_MASK, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)

        compressAndDecompressTree(wsWithMaskForDelete, fixtureStack, remoteRobot)

        deletionOfUssPathRoot.dialogTitle = deletionOfUssPathRoot.dialogTitle.format(ussMask)
        deletionOfDSMask.dialogTitle = deletionOfDSMask.dialogTitle.format(zosUserDatasetMask)
        //test

        deleteWSFromContextMenu(ussMask, fixtureStack,remoteRobot)
        deleteWSFromContextMenu(zosUserDatasetMask, fixtureStack,remoteRobot)

        closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        return deleteWSFromContextMenu(wsWithMaskForDelete, fixtureStack, remoteRobot)
    }
}

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetViaContextMenuNoConnectionTest : IdeaInteractionClass(){
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add Working Set Dialog", "Edit Working Set Dialog", "Create Mask Dialog")
//    private var addWorkingSetDialog = AddWorkingSetSubDialog()
    override val wsName = "first ws"
    private lateinit var processManager: ProcessManager

    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        processManager = ProcessManager()
        addWorkingSetDialog = AddWorkingSetSubDialog(fixtureStack, remoteRobot)
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
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
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests to add new working set without connection, checks that correct message is returned.
     */
    @Test
    @Disabled("https://jira.ibagroup.eu/browse/IJMP-977")
    fun testAddWorkingSetWithoutConnectionViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            injectTestInfo(testInfo)
            injectTestInfoRestTopology(testInfo)

            callCreateWSFromContextMenu(fixtureStack, remoteRobot)
            try {
                if (addWorkingSetDialog.isShown()) {
                    Assertions.assertTrue(false)
                }
            } catch (e: WaitForConditionTimeoutException) {
                e.message.shouldContain("Failed to find 'Dialog' by 'Title Add Working Set Dialog'")
            } finally {
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
            }

            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createConnectionFromActionButton(closableFixtureCollector, fixtureStack)
                addConnectionDialog(fixtureStack) {
                    addConnection(
                        connectionName,
                        "https://${mockServer.hostName}:${mockServer.port}",
                        ZOS_USERID,
                        ZOS_PWD,
                        true
                    )
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)

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

}
