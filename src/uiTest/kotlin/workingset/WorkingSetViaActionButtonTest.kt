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
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.auxiliary.components.dialogs.AddWorkingSetSubDialog
import workingset.testutils.injectListEmptyData


/**
 * Tests creating working sets and masks via action button.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetViaActionButtonTest : IdeaInteractionClass() {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add Working Set Dialog")
    private val wsName1 = "WS1"
    private val wsName2 = "WS2"
    private val wsName3 = "WS3"
    private val wsName4 = "WS4"
    private val wsName5 = "WS5"
    private lateinit var processManager: ProcessManager

    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot, testInfo:TestInfo) {
        processManager = ProcessManager()
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(testInfo, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        addWorkingSetDialog = AddWorkingSetSubDialog(fixtureStack, remoteRobot)
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
    fun testAddEmptyWorkingSetWithVeryLongNameViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName: String = "B".repeat(200)
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        find<HeavyWeightWindowFixture>(messageLoc).findText(EMPTY_DATASET_MESSAGE)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }

    /**
     * Tests to add new working set with one valid mask.
     */
    @Test
    fun testAddWorkingSetWithOneValidMaskViaActionButton(remoteRobot: RemoteRobot){
        val mask = Pair("$ZOS_USERID.*", "z/OS")
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName1, mask, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }

    /**
     * Tests to add new working set with several valid z/OS masks, opens masks in explorer.
     */
    @Test
    fun testAddWorkingSetWithValidZOSMasksViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot){

        val masks: ArrayList<Pair<String, String>> = ArrayList()
        //todo allocate dataset with 44 length when 'Allocate Dataset Dialog' implemented

        validZOSMasks.forEach {
            masks.add(Pair(it, ZOS_MASK))
        }
        injectListEmptyData(testInfo)
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName2, masks, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        Thread.sleep(3000)
        validZOSMasks.forEach {
            openWSOpenMaskInExplorer(
                wsName2,
                it.uppercase(),
                fixtureStack,
                remoteRobot
            )
        }
    }

    /**
     * Tests to add new working set with several valid USS masks, opens masks in explorer.
     */
    @Test
    fun testAddWorkingSetWithValidUSSMasksViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot){

        val masks: ArrayList<Pair<String, String>> = ArrayList()

        validUSSMasks.forEach {
            masks.add(Pair(it, USS_MASK))
        }
        injectListEmptyData(testInfo, false)
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName3, masks, fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        compressWsIfcDecompressed(wsName3,validUSSMasks[0], fixtureStack, remoteRobot)
        validUSSMasks.forEach { openWSOpenMaskInExplorer(wsName3, it, fixtureStack, remoteRobot) }
    }

    /**
     * Tests to add new working set with invalid masks, checks that correct messages are returned.
     */
    @Test
    fun testAddWorkingSetWithInvalidMasksViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {

        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        maskMessageMap.forEach {
            val mask = Pair(it.key, ZOS_MASK)
            addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName4, mask, fixtureStack, remoteRobot)
            if(isButtonEnableByTextAddWorkingSet(OK_TEXT, fixtureStack, remoteRobot)){
                clickByText(OK_TEXT, fixtureStack,remoteRobot)
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
            assertFalse(isButtonEnableByTextAddWorkingSet(OK_TEXT, fixtureStack, remoteRobot))
            clickToByTextAddWorkingSet(it.key.uppercase(), fixtureStack,remoteRobot)
            Thread.sleep(3000)
            clickActionButtonByXpath(removeButtonLoc, fixtureStack, remoteRobot)
        }
        clickByText(CANCEL_TEXT, fixtureStack,remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }

    /**
     * Tests to add working set with the same masks, checks that correct message is returned.
     */
    @Test
    fun testAddWorkingSetWithTheSameMasksViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        callCreateWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName5, Pair("$ZOS_USERID.*", "z/OS"), fixtureStack, remoteRobot)
        addWorkingSetDialog.fillAddWorkingSet(connectionName, wsName5, Pair("$ZOS_USERID.*", "z/OS"), fixtureStack, remoteRobot)
        clickByText(OK_TEXT, fixtureStack,remoteRobot)
        Thread.sleep(2000)
        find<HeavyWeightWindowFixture>(messageLoc).findText(IDENTICAL_MASKS_MESSAGE)
        assertFalse(isButtonEnableByTextAddWorkingSet(OK_TEXT, fixtureStack, remoteRobot))
        clickByText(CANCEL_TEXT, fixtureStack,remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
    }
}

/**
 * Tests creating working sets and masks via action button without creating connection
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetViaActionButtonNoConnectionTest : IdeaInteractionClass(){
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private lateinit var processManager: ProcessManager

    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot){
        startMockServer()
        processManager = ProcessManager()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
    }

    @AfterAll
    fun tearDownAll(){
        processManager.close()
        mockServer.shutdown()
    }

    @Test
    @Disabled("https://jira.ibagroup.eu/browse/IJMP-977")
    fun testAddWorkingSetWithoutConnectionViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val wsName = "first ws"
            responseDispatcher.injectTestInfo(testInfo)
            responseDispatcher.injectTestInfoRestTopology(testInfo)
            callCreateWorkingSetFromActionButton(fixtureStack,remoteRobot)

            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                try {
                    if (dialog("Add Working Set Dialog").isShowing) {
                        Assertions.assertTrue(false)
                    }
                } catch (e: WaitForConditionTimeoutException) {
                    e.message.shouldContain("Failed to find 'Dialog' by 'dialogTitle Add Working Set Dialog'")
                } finally {
                    closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                }

                createConnectionFromActionButton(closableFixtureCollector, fixtureStack)
                fillConnectionFields(fixtureStack=fixtureStack, remoteRobot=remoteRobot)
                clickByText(OK_TEXT, fixtureStack, remoteRobot)
                callCreateWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
                addWorkingSetDialog(fixtureStack) {
                    addWorkingSet(wsName, connectionName)
                    clickByText(OK_TEXT, fixtureStack, remoteRobot)
                    if (dialog("Add Working Set Dialog").isShowing) {
                        Assertions.assertTrue(false)
                    }
                    clickByText(OK_TEXT, fixtureStack, remoteRobot)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
            }
        }

}
