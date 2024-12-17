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

package jes

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ActionButtonFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.*
import workingset.auxiliary.components.dialogs.AddJesWorkingSetSubDialog
import workingset.testutils.injectEmptyJobList
import workingset.testutils.injectInvalidUrlPortInfo
import workingset.*

import java.time.Duration


/**
 * Tests creating JES working sets and filters via action button.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class JesWorkingSetViaActionButtonTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add JES Working Set Dialog")
    private var utilObject = IdeaInteractionClass()
    private val connectionName = "valid connection"
    private var addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog()
    private val jwsNameV1 = "JWS1"
    private val jwsNameV2 = "JWS2"
    private val jwsNameV3 = "JWS3"
    private val jwsNameV4 = "JWS4"
    private val jwsNameV5 = "JWS5"
    private lateinit var processManager: ProcessManager

    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createValidConnectionWithMock(testInfo, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog(fixtureStack, remoteRobot)
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
     * Tests to add new empty JES working set with very long name, checks that correct message is returned.
     */
    @Test
    @Disabled("waiting for the fix to be applied https://jira.ibagroup.eu/browse/IJMP-1498")
    fun testAddEmptyJesWorkingSetWithVeryLongNameViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(B_200, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(messageLoc).findText(EMPTY_DATASET_MESSAGE)
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with one valid filter.
     */
    @Test
    fun testAddJesWorkingSetWithOneValidFilterViaActionButton(remoteRobot: RemoteRobot) {
        utilObject.callCreateJesWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName,jwsNameV1, filterAllAndZos, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with several valid filters, opens filters in explorer.
     */
    @Test
    fun testAddJesWorkingSetWithValidFiltersViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot){
        injectEmptyJobList(testInfo)
        utilObject.callCreateJesWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName,jwsNameV2, validJobsFilters, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
        utilObject.decompressWsIfCompressed(jwsNameV2,prefixAndOwnerPattern.format(validJobsFilters.first().first, validJobsFilters.first().second),fixtureStack,remoteRobot)
        validJobsFilters.forEach {

                utilObject.checkJesErrorInTrees(it, "", fixtureStack, remoteRobot)
                utilObject.closeFilterInExplorer(it, fixtureStack, remoteRobot)
            }
            openOrCloseJesWorkingSetInExplorer(jwsNameV2, fixtureStack, remoteRobot)
        }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with invalid filters, checks that correct messages are returned.
     */
    @Test
    fun testAddJesWorkingSetWithInvalidFiltersViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callCreateJesWorkingSetFromActionButton(fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.setConnectionName(connectionName,fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.setWsName(jwsNameV3,fixtureStack, remoteRobot)
        invalidJobsFiltersMap.forEach {
            addJesWorkingSetSubDialog.setJobFilter(it.key.first, remoteRobot)
            if (addJesWorkingSetSubDialog.okButton.isEnabled()) {
                addJesWorkingSetSubDialog.okButton.click()
            } else {
                addJesWorkingSetSubDialog.okButton.moveMouse()
            }
            val textToMoveMouse = when (it.key.second) {
                1 -> it.key.first.first
                2 -> it.key.first.second.uppercase()
                else -> it.key.first.third
            }
            find<ComponentFixture>(dialogRootPaneLoc).findText(textToMoveMouse).moveMouse()
            find<HeavyWeightWindowFixture>(errorTipsTextLoc, Duration.ofSeconds(5)).findText(it.value)
            assertFalse(addJesWorkingSetSubDialog.okButton.isEnabled())
            find<JTextFieldFixture>(dialogRootPaneLoc).findAllText(PREFIX_WORD).last().click()
            addJesWorkingSetSubDialog.removeButton.click()
        }
        addJesWorkingSetSubDialog.cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add JES working set with the same filters, checks that correct message is returned.
     */
    @Test
    fun testAddJesWorkingSetWithTheSameFiltersViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callCreateJesWorkingSetFromActionButton(fixtureStack, remoteRobot)
        val filters = listOf(filterAllAndZos,filterAllAndZos)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName,jwsNameV4,filters,fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()

        find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findText(IDENTICAL_FILTERS_MESSAGE)
        assertFalse(addJesWorkingSetSubDialog.okButton.isEnabled())
        addJesWorkingSetSubDialog.cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add JES working set with invalid connection, checks that correct message is returned.
     */
    @Test
    fun testAddJWSWithInvalidConnectionViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            injectInvalidUrlPortInfo(testInfo, INVALID_PORT_104431)
            injectEmptyJobList(testInfo)

            createConnection(
                fixtureStack,
                closableFixtureCollector,
                "invalid_connection",
                false,
                remoteRobot,
                "https://${mockServer.hostName}:$INVALID_PORT_104431"
            )
            utilObject.callCreateJesWorkingSetFromActionButton(fixtureStack, remoteRobot)
            addJesWorkingSetSubDialog.fillAddJobFilter(connectionName,jwsNameV5,filterAllAndZos,fixtureStack, remoteRobot)
            addJesWorkingSetSubDialog.okButton.click()
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)

            openOrCloseJesWorkingSetInExplorer(jwsNameV5, fixtureStack, remoteRobot)
            findAll<ComponentFixture>(invalidPortInTreesLoc).forEach {
                it.click()
                findAll<ActionButtonFixture>(errorCloseIconLoc).first().click()
            }
            openOrCloseJesWorkingSetInExplorer(jwsNameV5, fixtureStack, remoteRobot)
        }
}
