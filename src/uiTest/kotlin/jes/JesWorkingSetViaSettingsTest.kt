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
import com.intellij.remoterobot.search.locators.byXpath
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.*
import workingset.auxiliary.components.dialogs.AddJesWorkingSetSubDialog
import workingset.auxiliary.components.dialogs.EditJesWorkingSetSubDialog
import workingset.auxiliary.components.dialogs.SettingsDialogUtil
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.*
import java.time.Duration

/**
 * Tests creating JES working sets and jobs filters via settings.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class JesWorkingSetViaSettingsTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Settings Dialog", "Add JES Working Set Dialog", "Edit JES Working Set Dialog"
    )
    private val connectionName = "valid connection"
    private var utilObject = IdeaInteractionClass()
    private var settingsDialog = SettingsDialogUtil()
    private var addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog()
    private var editJesWorkingSetSubDialog = EditJesWorkingSetSubDialog()
    private var okButtonSub = ButtonElement()
    private var canselButtonSub = ButtonElement()

    private val invalidConnectionName = "invalid connection"
    private val newUniqueName = "Unique"
    private val newUniqueName2 = "Unique2"
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
        createConnection(fixtureStack, closableFixtureCollector, invalidConnectionName, false,
            remoteRobot, "https://${mockServer.hostName}:$PORT_10443"
        )
        settingsDialog = SettingsDialogUtil(fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog(fixtureStack, remoteRobot)
        editJesWorkingSetSubDialog = EditJesWorkingSetSubDialog(fixtureStack, remoteRobot)
        okButtonSub = ButtonElement(subOkButtonLoc, fixtureStack, remoteRobot)
        canselButtonSub = ButtonElement(subCancelButtonLoc, fixtureStack, remoteRobot)
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
        utilObject.removeAllJwsWorkingSets(fixtureStack,remoteRobot)
    }

    /**
     * Tests to add new JES working set without connection, checks that correct message is returned.
     */
    @Test
    @Disabled("out of date, we can't create ws without connection")
    fun testAddJesWorkingSetWithoutConnectionViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet("JWS1", "")
                    clickButton("OK")
                    comboBox("Specify connection").click()
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText("You must provide a connection")
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        }
    }

    /**
     * Tests to add new empty JES working sets with different names, checks that correct message is returned.
     */
    @Test
    fun testAddEmptyJesWorkingSetsWithDifferentNamesViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot){

        createJwsInSettings(testInfo,B_200,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.callAddWs(fixtureStack,remoteRobot,closableFixtureCollector)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName,SPEC_SYMBOLS,filterAllAndZos,fixtureStack,remoteRobot)
        okButtonSub.click()
        settingsDialog.okButton.click()
    }

    /**
     * Tests to add new JES working set with one valid jobs filter.
     */
    @Test
    fun testAddJesWorkingSetWithOneValidFilterViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        createJwsInSettings(testInfo, jwsNameV1, remoteRobot)
        closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
    }

    /**
     * Tests to add new JES working set with already existing name, checks that correct message is returned.
     */
    @Test
    fun testAddJWSWithTheSameNameViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot)= with(remoteRobot) {
        createJwsInSettings(testInfo, jwsNameV2, remoteRobot)
        createJwsInSettings(testInfo, jwsNameV2, remoteRobot)
        val message = find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_WORKING_SET_NAME.format(jwsNameV2))
        assertFalse(okButtonSub.isEnabled())
        canselButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        settingsDialog.okButton.click()
    }

    /**
     * Tests to add new JES working set with invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    fun testAddJWSWithInvalidFiltersViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.callAddWs(fixtureStack,remoteRobot,closableFixtureCollector)
        invalidJobsFiltersMap.forEach {
            addJesWorkingSetSubDialog.fillAddJobFilter(connectionName,jwsNameV3,it.key.first,fixtureStack,remoteRobot)

            if (okButtonSub.isEnabled()) {
                okButtonSub.click()
            } else {
                okButtonSub.moveMouse()
            }
            val textToMoveMouse = when (it.key.second) {
                1 -> it.key.first.first
                2 -> it.key.first.second.uppercase()
                else -> it.key.first.third
            }
            find<HeavyWeightWindowFixture>(addJesWorkingSetDialogLoc).findText(textToMoveMouse).moveMouse()
            find<HeavyWeightWindowFixture>(helpLoc,Duration.ofSeconds(30)).findText(it.value)
            assertFalse(okButtonSub.isEnabled())
            findAll<JTextFieldFixture>(dialogRootPaneLoc).last().findAllText(PREFIX_WORD).last().click()

            addJesWorkingSetSubDialog.removeButtonAlt.click()

        }
        canselButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
        settingsDialog.cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
    }

    /**
     * Tests to add new JES working set with several valid jobs filters, opens filters in explorer.
     */
    @Test
    fun testAddJWSWithValidFiltersViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJwsInSettings(testInfo, jwsNameV4, remoteRobot, validJobsFilters)
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        openOrCloseJesWorkingSetInExplorer(jwsNameV4, fixtureStack,  remoteRobot)
        validJobsFilters.forEach {
            utilObject.openJobFilterInExplorer(it, "", fixtureStack, remoteRobot)
            closeFilterInExplorer(it, fixtureStack, remoteRobot)
        }
        openOrCloseJesWorkingSetInExplorer(jwsNameV4, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new JES working set with invalid connection, checks that correct message is returned.
     */
    @Test
    fun testAddJWSWithInvalidConnectionViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectInvalidUrlPortInfo(testInfo,PORT_10443)
        injectEmptyJobList(testInfo)
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.callAddWs(fixtureStack,remoteRobot,closableFixtureCollector)
        addJesWorkingSetSubDialog.fillAddJobFilter(invalidConnectionName,jwsNameV5, filterAllAndZos,fixtureStack,remoteRobot)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
        settingsDialog.okButton.click()

        openOrCloseJesWorkingSetInExplorer(jwsNameV5, fixtureStack, remoteRobot)
        findAll<ComponentFixture>(invalidPortInTreesLoc).forEach {
            it.click()
            findAll<ActionButtonFixture>(errorCloseIconLoc).first().click()
        }
        openOrCloseJesWorkingSetInExplorer(jwsNameV5, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new JES working set with the same jobs filters, checks that correct message is returned.
     */
    @Test
    fun testAddJWSWithTheSameFiltersViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        createJwsInSettings(testInfo, jwsNameV6, remoteRobot, listOf(filterAllAndZos, filterAllAndZos))
        find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findText(IDENTICAL_FILTERS_MESSAGE)
        assertFalse(okButtonSub.isEnabled())
        canselButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
    }

    /**
     * Tests to edit JES working set by adding one job filter, checks that jws is refreshed, opens new filter.
     */
    @Test
    fun testEditJWSAddOneFilterViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJwsInSettings(testInfo, jwsNameV7, remoteRobot)
        settingsDialog.okButton.click()
        openOrCloseJesWorkingSetInExplorer(jwsNameV7, fixtureStack, remoteRobot)
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.selectWs(jwsNameV7, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editJesWorkingSetSubDialog.setJobFilter(filterAllAndZosAlt, remoteRobot)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        utilObject.openJesExplorerTab(fixtureStack, remoteRobot)
        utilObject.refreshWorkSpace(jwsNameV7, fixtureStack,remoteRobot)
        utilObject.decompressWsIfCompressed(jwsNameV7, convertJobFilterToString(filterAllAndZosAlt), fixtureStack, remoteRobot)
        utilObject.openJobFilterInExplorer(filterAllAndZosAlt, "", fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsNameV7, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit JES working set by deleting several filters, checks that jws is refreshed and filters were deleted.
     */
    @Test
    fun testEditJWSDeleteFiltersViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJwsInSettings(testInfo, jwsNameV8, remoteRobot, validJobsFilters)
        val filterForRemove = validJobsFilters.slice(1..3)
        settingsDialog.okButton.click()
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.selectWs(jwsNameV8, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editJesWorkingSetSubDialog.deleteFilters(filterForRemove, fixtureStack, remoteRobot)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        utilObject.openJesExplorerTab(fixtureStack, remoteRobot)
        utilObject.refreshWorkSpace(jwsNameV8, fixtureStack,remoteRobot)
        filterForRemove.forEach { checkFilterWasDeletedJWSRefreshed(it, fixtureStack, remoteRobot) }
        openOrCloseJesWorkingSetInExplorer(jwsNameV8, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit JES working set by deleting all filters, checks that jws is refreshed and filters were deleted.
     */
    @Test
    fun testEditJWSDeleteAllFiltersViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJwsInSettings(testInfo, jwsNameV9, remoteRobot, validJobsFilters)
        settingsDialog.okButton.click()
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.selectWs(jwsNameV9, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editJesWorkingSetSubDialog.deleteAllFilters(remoteRobot)
        okButtonSub.click()
        okButtonSub.click()
        settingsDialog.okButton.click()
        utilObject.openJesExplorerTab(fixtureStack,remoteRobot)
        utilObject.refreshWorkSpace(jwsNameV9, fixtureStack,remoteRobot)
        validJobsFilters.forEach { checkFilterWasDeletedJWSRefreshed(it, fixtureStack, remoteRobot) }
        openOrCloseJesWorkingSetInExplorer(jwsNameV9, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit JES working set by changing connection to invalid, checks that correct message is returned.
     */
    @Test
    fun testEditJWSChangeConnectionToInvalidViaSettings(testInfo: TestInfo,remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJwsInSettings(testInfo, jwsNameV10, remoteRobot, filterAllAndZos)
        settingsDialog.selectWs(jwsNameV10, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editJesWorkingSetSubDialog.setConnectionName(invalidConnectionName, fixtureStack, remoteRobot)
        okButtonSub.click()
        settingsDialog.okButton.click()
        utilObject.openJesExplorerTab(fixtureStack,remoteRobot)
        utilObject.decompressWsIfCompressed(jwsNameV10,
            convertJobFilterToString(filterAllAndZos),fixtureStack, remoteRobot)
        utilObject.compressAndDecompressTree(convertJobFilterToString(filterAllAndZos),fixtureStack, remoteRobot)

        findAll<ComponentFixture>(invalidPortInTreesLoc).forEach {
            it.click()
            findAll<ActionButtonFixture>(errorCloseIconLoc).first().click()
        }
    }

    /**
     * Tests to edit JES working set by changing connection from invalid to valid, checks that jws is refreshed in explorer and error message disappeared.
     */
    @Test
    fun testEditJWSChangeConnectionToNewValidViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot){
            createJwsInSettings(testInfo, jwsNameV12, remoteRobot, filterAllAndZos,invalidConnectionName)
            injectTestInfo(testInfo)
            injectTestInfoRestTopology(testInfo)
            injectEmptyJobList(testInfo)
            settingsDialog.selectWs(jwsNameV12, fixtureStack, remoteRobot)
            settingsDialog.editWsButton.click()
            editJesWorkingSetSubDialog.setConnectionName(connectionName, fixtureStack, remoteRobot)
            okButtonSub.click()
            settingsDialog.okButton.click()
            utilObject.openJesExplorerTab(fixtureStack, remoteRobot)
            utilObject.decompressWsIfCompressed(jwsNameV12,convertJobFilterToString(filterAllAndZos), fixtureStack, remoteRobot)
            checkItemWasDeletedWSRefreshed(invalidPort104431, fixtureStack, remoteRobot)

        }

    /**
     * Tests to edit JES working set by renaming it, checks that jws is refreshed in explorer.
     */
    @Test
    fun testEditJWSRenameViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJwsInSettings(testInfo, newUniqueName, remoteRobot, filterAllAndZos)
        createJwsInSettings(testInfo, jwsNameV13, remoteRobot, filterAllAndZos)
        settingsDialog.selectWs(newUniqueName, fixtureStack, remoteRobot)
        settingsDialog.editWsButton.click()
        editJesWorkingSetSubDialog.setWsName(jwsNameV13, fixtureStack, remoteRobot)
        okButtonSub.click()
        val message = find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_WORKING_SET_NAME.format(jwsNameV13))
        editJesWorkingSetSubDialog.setWsName(newUniqueName2, fixtureStack, remoteRobot)
        okButtonSub.click()
        closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        checkItemWasDeletedWSRefreshed(newUniqueName, fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(newUniqueName2, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete JES working set, checks that explorer info is refreshed.
     */
    @Test
    fun testDeleteJWSViaSettings(testInfo: TestInfo, remoteRobot: RemoteRobot){
        createJwsInSettings(testInfo, jwsNameV14, remoteRobot, filterAllAndZos)
        settingsDialog.okButton.click()
        utilObject.compressAndDecompressTree(jwsNameV14, fixtureStack, remoteRobot)
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.selectWs(jwsNameV14, fixtureStack, remoteRobot)
        settingsDialog.removeButton.click()
        settingsDialog.okButton.click()
        checkItemWasDeletedWSRefreshed(jwsNameV14, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete all JES working sets, checks that explorer info is refreshed.
     */
    @Test
    fun testDeleteAllJWSViaSettings(testInfo: TestInfo,remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJwsInSettings(testInfo, jwsNameV15, remoteRobot, filterAllAndZos)
        createJwsInSettings(testInfo, jwsNameV16, remoteRobot, filterAllAndZos)
        createJwsInSettings(testInfo, jwsNameV17, remoteRobot, filterAllAndZos)
        settingsDialog.okButton.click()
        utilObject.openJesExplorerTab(fixtureStack,remoteRobot)
        utilObject.compressAndDecompressTree(jwsNameV15, fixtureStack, remoteRobot)
        utilObject.compressAndDecompressTree(jwsNameV16, fixtureStack, remoteRobot)
        utilObject.compressAndDecompressTree(jwsNameV17, fixtureStack, remoteRobot)
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.selectWs(jwsNameV15, fixtureStack, remoteRobot)
        utilObject.doSelectAll(fixtureStack,remoteRobot)
        settingsDialog.removeButton.click()
        settingsDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(settingsDialog.dialogTitle)
        assertTrue(find<ComponentFixture>(viewTree).hasText(NOTHING_TO_SHOW_MSG))

    }

    private fun createJwsInSettings(testInfo:TestInfo, jwsName: String, remoteRobot:RemoteRobot, filet: Triple<String, String, String> =filterAllAndZos, connection:String=connectionName){
        injectEmptyJobList(testInfo)
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.callAddWs(fixtureStack,remoteRobot,closableFixtureCollector)
        addJesWorkingSetSubDialog.fillAddJobFilter(connection,jwsName,filet,fixtureStack,remoteRobot)
        okButtonSub.click()
    }

    private fun createJwsInSettings(testInfo:TestInfo, jwsName: String, remoteRobot:RemoteRobot, filet: List<Triple<String, String, String>> ){
        injectEmptyJobList(testInfo)
        utilObject.callSettingsByAction(fixtureStack,remoteRobot)
        settingsDialog.jesWorkingSetTab.click()
        settingsDialog.callAddWs(fixtureStack,remoteRobot,closableFixtureCollector)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName,jwsName,filet,fixtureStack,remoteRobot)
        okButtonSub.click()
    }
}
