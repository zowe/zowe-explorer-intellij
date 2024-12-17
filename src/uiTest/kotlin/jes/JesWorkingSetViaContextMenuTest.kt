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
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import testutils.ProcessManager
import workingset.*
import workingset.auxiliary.components.dialogs.AddJesWorkingSetSubDialog
import workingset.auxiliary.components.dialogs.CreateJobFilterSubDialog
import workingset.auxiliary.components.dialogs.EditJesWorkingSetSubDialog
import workingset.auxiliary.components.dialogs.EditJobFilterSubDialog
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.injectEmptyJobList
import workingset.testutils.injectInvalidUrlPortInfo
import workingset.testutils.injectTestInfo
import workingset.testutils.injectTestInfoRestTopology
import workingset.*
import java.time.Duration

/**
 * Tests creating JES working sets and jobs filters from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class JesWorkingSetViaContextMenuTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var utilObject = IdeaInteractionClass()
    private var addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog()
    private var createJobFilterSubDialog = CreateJobFilterSubDialog()
    private var editJesWorkingSetSubDialog = EditJesWorkingSetSubDialog()
    private var editJobFilterSubDialog = EditJobFilterSubDialog()

    private var wantToClose = mutableListOf(
        "Add JES Working Set Dialog", "Edit JES Working Set Dialog", "Edit Jobs Filter Dialog"
    )
    private val connectionName = "valid connection"
    private val connectionName2 = "valid connection2"
    private val invalidConnectionName = "invalid connection"
    private var yesButton = ButtonElement()
    private val jwsNameV16a = "JWS16alt"
    private val jwsNameV16b = "JWS16new"
    private lateinit var processManager: ProcessManager


    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        addJesWorkingSetSubDialog = AddJesWorkingSetSubDialog(fixtureStack, remoteRobot)
        createJobFilterSubDialog = CreateJobFilterSubDialog(fixtureStack, remoteRobot)
        editJesWorkingSetSubDialog = EditJesWorkingSetSubDialog(fixtureStack, remoteRobot)
        editJobFilterSubDialog = EditJobFilterSubDialog(fixtureStack, remoteRobot)
        yesButton = ButtonElement(YES_TEXT,fixtureStack, remoteRobot)
        createValidConnectionWithMock(testInfo, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
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
        utilObject.removeAllJwsWorkingSets(fixtureStack,remoteRobot)
    }

    /**
     * Tests to add new JES working set without connection, checks that correct message is returned.
     */
    @Disabled("out of date")
    @Test
    fun testAddJesWorkingSetWithoutConnectionViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            injectTestInfo(testInfo)
            injectTestInfoRestTopology(testInfo)


            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
                try {
                    if (dialog("Add JES Working Set Dialog").isShowing) {
                        assertTrue(false)
                    }
                } catch (e: WaitForConditionTimeoutException) {
                    e.message.shouldContain("Failed to find 'Dialog' by 'title Add JES Working Set Dialog'")
                } finally {
                    closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                }

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

                createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsNameV1, connectionName)
                    clickButton("OK")
                    Thread.sleep(3000)
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                        EMPTY_DATASET_MESSAGE
                    )
                    clickButton("OK")
                    Thread.sleep(3000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
            }
        }

    /**
     * Tests to add new empty JES working sets with different names, checks that correct message is returned.
     */
    @ParameterizedTest
    @ValueSource(strings = [B_200_CONST, SPEC_SYMBOLS])
    fun testAddEmptyJesWorkingSetsWithDifferentNamesViaContextMenu(jwtName: String, remoteRobot: RemoteRobot){
        utilObject.callCreateJWSFromContextMenu(fixtureStack, closableFixtureCollector, remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwtName, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()
        addJesWorkingSetSubDialog.okButton.click()
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with one valid jobs filter.
     */
    @Test
    fun testAddJesWorkingSetWithOneValidFilterViaContextMenu(remoteRobot: RemoteRobot){
        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV2, filterAllAndZos, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)

    }

    /**
     * Tests to add new JES working set with already existing name, checks that correct message is returned.
     */
    @Test
    fun testAddJWSWithTheSameNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot)  {

        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV3, filterAllAndZos, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()

        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV3, filterAllAndZos, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()

        val message = find<ContainerFixture>(messageLoc,Duration.ofSeconds(30)).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_WORKING_SET_NAME.format(jwsNameV3))
        assertFalse(addJesWorkingSetSubDialog.okButton.isEnabled())
        addJesWorkingSetSubDialog.cancelButton.click()
    }

    /**
     * Tests to add new JES working set with invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    fun testAddJWSWithInvalidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {

        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)

        invalidJobsFiltersMap.forEach {
            addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV4, it.key.first, fixtureStack, remoteRobot)
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
            find<HeavyWeightWindowFixture>(addJesWorkingSetDialogLoc).findText(textToMoveMouse).moveMouse()
            find<HeavyWeightWindowFixture>(helpLoc,Duration.ofSeconds(30)).findText(it.value)
            assertFalse(addJesWorkingSetSubDialog.okButton.isEnabled())
            ButtonElement(removeButtonLoc, fixtureStack, remoteRobot).click()
        }
        addJesWorkingSetSubDialog.cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with several valid jobs filters, opens filters in explorer.
     */
    @Test
    fun testAddJWSWithValidFiltersViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
        injectEmptyJobList(testInfo)
        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV5, validJobsFilters, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
        utilObject.decompressWsIfCompressed(
            jwsNameV5,
            prefixAndOwnerPattern.format(validJobsFilters.first().first, validJobsFilters.first().second),
            fixtureStack,remoteRobot)
        validJobsFilters.forEach {
            utilObject.openJobFilterInExplorer(it, "", fixtureStack, remoteRobot)
            closeFilterInExplorer(it, fixtureStack, remoteRobot)
        }
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with invalid connection, checks that correct message is returned.
     */
    @Test
    fun testAddJWSWithInvalidConnectionViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            injectInvalidUrlPortInfo(testInfo,PORT_10443)
            injectEmptyJobList(testInfo)
            createConnection(
                fixtureStack,
                closableFixtureCollector,
                invalidConnectionName,
                false,
                remoteRobot,
                "https://${mockServer.hostName}:$PORT_10443"
            )
            utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
            addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV6, validJobsFilters, fixtureStack, remoteRobot)
            addJesWorkingSetSubDialog.okButton.click()
            closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)

            utilObject.compressAndDecompressTree(jwsNameV6, fixtureStack, remoteRobot)
            findAll<ComponentFixture>(invalidPortInTreesLoc).forEach {
                it.click()
                findAll<ActionButtonFixture>(errorCloseIconLoc).first().click()
            }
            utilObject.compressAndDecompressTree(jwsNameV6, fixtureStack, remoteRobot)
        }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with the same jobs filters, checks that correct message is returned.
     */
    @Test
    fun testAddJWSWithTheSameFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV7, filterAllAndZos, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsNameV7, filterAllAndZos, fixtureStack, remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()

        find<HeavyWeightWindowFixture>(messageLoc,Duration.ofSeconds(30)).findText(IDENTICAL_FILTERS_MESSAGE)
        assertFalse(addJesWorkingSetSubDialog.okButton.isEnabled())
        addJesWorkingSetSubDialog.cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(addJesWorkingSetSubDialog.dialogTitle)
    }

    /**
     * Tests to create invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    fun testCreateInvalidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        invalidJobsFiltersMap.forEach {
            addJesWorkingSetSubDialog.setWsName(jwsNameV8,fixtureStack,remoteRobot)
            addJesWorkingSetSubDialog.setJobFilter(it.key.first, remoteRobot)
            if (addJesWorkingSetSubDialog.okButton.isEnabled()) {
                addJesWorkingSetSubDialog.okButton.click()
            }
            val textToMoveMouse = when (it.key.second) {
                1 -> it.key.first.first
                2 -> it.key.first.second
                else -> it.key.first.third
            }
            find<HeavyWeightWindowFixture>(myDialogXpathLoc).findText(textToMoveMouse).moveMouse()
            find<HeavyWeightWindowFixture>(errorTipsTextLoc, Duration.ofSeconds(3)).findText(it.value)
            assertFalse(addJesWorkingSetSubDialog.okButton.isEnabled())
            addJesWorkingSetSubDialog.removeButton.click()
        }
        addJesWorkingSetSubDialog.cancelButton.click()
    closableFixtureCollector.closeOnceIfExists(CreateJobsFilterDialog.name)
    }


    /**
     * Tests to create valid jobs filters from context menu.
     */
    @Test
    fun testCreateValidFiltersViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
        createAndExpendJws(testInfo,jwsNameV9, remoteRobot, validJobsFilters)
        utilObject.decompressWsIfCompressed(jwsNameV9,prefixAndOwnerPattern.format(validJobsFilters.first().first, validJobsFilters.first().second),fixtureStack,remoteRobot)
        validJobsFilters.forEach {
            utilObject.openJobFilterInExplorer(it, "", fixtureStack, remoteRobot)
            utilObject.closeFilterInExplorer(it, fixtureStack, remoteRobot)
        }
        openOrCloseJesWorkingSetInExplorer(jwsNameV9, fixtureStack, remoteRobot)
    }

    /**
     * Tests to create already existing jobs filter in JES working set, checks that correct message is returned.
     */
    @Test
    fun testCreateAlreadyExistsFilterViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        createAndExpendJws(testInfo,jwsNameV10, remoteRobot)
        utilObject.callCreateJobsFilterFromContextMenu(jwsNameV10,fixtureStack,closableFixtureCollector,remoteRobot)
        createJobFilterSubDialog.createJobsFilter(filterAllAndZos, remoteRobot)
        createJobFilterSubDialog.okButton.click()
        assertFalse(createJobFilterSubDialog.okButton.isEnabled())
        assertTrue(
            find<HeavyWeightWindowFixture>(messageLoc, Duration.ofSeconds(30)
            ).findAllText().first().text == UNIQUE_JOB_FILTER
        )
        createJobFilterSubDialog.cancelButton.click()
        closableFixtureCollector.closeOnceIfExists(createJobFilterSubDialog.dialogTitle)

        }


    /**
     * Tests to edit already existing JES working set by adding one jobs filter.
     */
    @Test
    fun testEditJWSAddOneFilterViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
        createAndExpendJws(testInfo,jwsNameV11, remoteRobot)
        utilObject.callEditJWSFromContextMenu(jwsNameV11, fixtureStack,closableFixtureCollector,remoteRobot)
        editJesWorkingSetSubDialog.setJobFilter(filterAllAndZosAlt, remoteRobot)
        editJesWorkingSetSubDialog.okButton.click()
        utilObject.decompressWsIfCompressed(jwsNameV11, convertJobFilterToString(filterAllAndZos), fixtureStack, remoteRobot)
        utilObject.openJobFilterInExplorer(filterAllAndZosAlt, "", fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsNameV11, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit already existing JES working set by deleting one jobs filter.
     */
    @Test
    fun testEditJWSDeleteJobsFilterViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
        createAndExpendJws(testInfo,jwsNameV12, remoteRobot)
        utilObject.callEditJWSFromContextMenu(jwsNameV12, fixtureStack,closableFixtureCollector,remoteRobot)
        editJesWorkingSetSubDialog.setJobFilter(filterAllAndZosAlt, remoteRobot)
        editJesWorkingSetSubDialog.removeButton.click()
        editJesWorkingSetSubDialog.okButton.click()
        checkFilterWasDeletedJWSRefreshed(filterAllAndZosAlt, fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsNameV12, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit already existing JES working set by deleting all jobs filter.
     */
    @Test
    @Disabled("waiting for the fix to be applied https://jira.ibagroup.eu/browse/IJMP-1498")
    fun testEditJWSDeleteAllFiltersViaContextMenu(testInfo: TestInfo,remoteRobot: RemoteRobot) = with(remoteRobot) {
        createAndExpendJws(testInfo,jwsNameV13, remoteRobot, deletedFilters)

        utilObject.callEditJWSFromContextMenu(jwsNameV13, fixtureStack,closableFixtureCollector,remoteRobot)
        editJesWorkingSetSubDialog.deleteAllFilters(remoteRobot)
        editJesWorkingSetSubDialog.okButton.click()
        find<HeavyWeightWindowFixture>(jobFilterTableHeaderLoc).findText(
            EMPTY_DATASET_MESSAGE
        )
        editJesWorkingSetSubDialog.okButton.click()
        closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
        deletedFilters.forEach { checkFilterWasDeletedJWSRefreshed(it, fixtureStack, remoteRobot) }
        openOrCloseJesWorkingSetInExplorer(jwsNameV13, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit already existing JES working set by changing connection to invalid, checks that correct message returned.
     */
    @Test
    @Disabled("waiting for the fix to be applied")
    fun testEditJWSChangeConnectionToInvalidViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        createAndExpendJws(testInfo,jwsNameV14, remoteRobot)
        createConnection(
            fixtureStack,closableFixtureCollector,invalidConnectionName,false,remoteRobot,"https://${mockServer.hostName}:$PORT_10443"
        )
        utilObject.callEditJWSFromContextMenu(jwsNameV14, fixtureStack,closableFixtureCollector,remoteRobot)
        editJesWorkingSetSubDialog.setConnectionName(invalidConnectionName,fixtureStack,remoteRobot)
        editJesWorkingSetSubDialog.okButton.click()

        closableFixtureCollector.closeOnceIfExists(editJesWorkingSetSubDialog.dialogTitle)
        findAll<ComponentFixture>(invalidPortInTreesLoc).forEach {
            it.click()
            findAll<ActionButtonFixture>(errorCloseIconLoc).first().click()
        }
        utilObject.openJobFilterInExplorer(
            filterAllAndZos,INVALID_URL_PORT.format(104431),fixtureStack,remoteRobot
        )
    }

    /**
     * Tests to edit already existing JES working set by changing connection to new one.
     */
    @Test
    fun testEditJWSChangeConnectionToNewValidViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
            createValidConnectionWithMock(testInfo, connectionName2, fixtureStack, closableFixtureCollector, remoteRobot)
            injectTestInfoRestTopology(testInfo)
            injectTestInfo(testInfo)
            injectEmptyJobList(testInfo)
            createAndExpendJws(testInfo,jwsNameV15, remoteRobot)
            utilObject.callEditJWSFromContextMenu(jwsNameV15, fixtureStack,closableFixtureCollector,remoteRobot)
            editJesWorkingSetSubDialog.setConnectionName(connectionName2,fixtureStack,remoteRobot)
            editJesWorkingSetSubDialog.okButton.click()
            closableFixtureCollector.closeOnceIfExists(editJesWorkingSetSubDialog.dialogTitle)
            checkItemWasDeletedWSRefreshed("Invalid URL port: \"104431\"", fixtureStack, remoteRobot)
            openOrCloseJesWorkingSetInExplorer(jwsNameV15, fixtureStack, remoteRobot)
        }

    /**
     * Tests to edit already existing JES working set by renaming it.
     */
    @Test
    fun testEditJWSRenameViaContextMenu(testInfo: TestInfo,remoteRobot: RemoteRobot) = with(remoteRobot) {
        createAndExpendJws(testInfo,jwsNameV16, remoteRobot)
        createAndExpendJws(testInfo,jwsNameV16a, remoteRobot)
        utilObject.callEditJWSFromContextMenu(jwsNameV16a, fixtureStack,closableFixtureCollector,remoteRobot)
        editJesWorkingSetSubDialog.setWsName(jwsNameV16,fixtureStack,remoteRobot)
        editJesWorkingSetSubDialog.okButton.click()
        val message = find<HeavyWeightWindowFixture>(
            messageLoc,
            Duration.ofSeconds(30)
        ).findAllText()
        (message[0].text + message[1].text).shouldContain(UNIQUE_WORKING_SET_NAME.format(jwsNameV16))
        editJesWorkingSetSubDialog.setWsName(jwsNameV16b,fixtureStack,remoteRobot)
        editJesWorkingSetSubDialog.okButton.click()
        checkItemWasDeletedWSRefreshed(jwsNameV16a, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit jobs filter in JES working set.
     */
    @Test
    fun testEditJobFilterViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
        injectEmptyJobList(testInfo)
        createAndExpendJws(testInfo,jwsNameV17, remoteRobot)
        utilObject.decompressWsIfCompressed(jwsNameV17, convertJobFilterToString(filterAllAndZos), fixtureStack, remoteRobot)
        utilObject.callEditJobFilter(filterAllAndZos, fixtureStack,closableFixtureCollector,remoteRobot)
        editJobFilterSubDialog.setJobsFilter(filterAllAndZosAlt, remoteRobot)
        editJobFilterSubDialog.okButton.click()
        checkFilterWasDeletedJWSRefreshed(filterAllAndZos, fixtureStack, remoteRobot)
        utilObject.openJobFilterInExplorer(filterAllAndZosAlt, "", fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsNameV17, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete jobs filter from JES working set.
     */
    @Test
    fun testDeleteJobFilterFromJWSViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        injectEmptyJobList(testInfo)
        val jobFilter=convertJobFilterToString(filterAllAndZos)
        createAndExpendJws(testInfo,jwsNameV18, remoteRobot)
        utilObject.decompressWsIfCompressed(jwsNameV18, jobFilter, fixtureStack, remoteRobot)
        utilObject.deleteJobFromContextMenu(jobFilter, fixtureStack,remoteRobot)
        yesButton.click()
        checkFilterWasDeletedJWSRefreshed(filterAllAndZos, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete JES working set.
     */
    @Test
    fun testDeleteJWSViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot){
        createAndExpendJws(testInfo,jwsNameV19, remoteRobot)
        utilObject.deleteJwsFromContextMenu(jwsNameV19, fixtureStack, remoteRobot)
        yesButton.click()
        checkFilterWasDeletedJWSRefreshed(filterAllAndZos, fixtureStack, remoteRobot)
        checkItemWasDeletedWSRefreshed(jwsNameV19, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete all JES working sets.
     */
    @Test
    fun testDeleteAllJWSViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot)= with(remoteRobot) {
        createAndExpendJws(testInfo,jwsNameV20, remoteRobot)
        utilObject.removeAllJwsWorkingSets(fixtureStack,remoteRobot)
        assertTrue(find<ComponentFixture>(viewTree).hasText(NOTHING_TO_SHOW_MSG))
    }

    private fun createAndExpendJws(testInfo:TestInfo, jwsName: String, remoteRobot:RemoteRobot, filet: Triple<String, String, String> =filterAllAndZos){
        injectEmptyJobList(testInfo)
        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsName, filet, fixtureStack,remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()
        utilObject.decompressWsIfCompressed(jwsName, filterAllAndZos.first, fixtureStack, remoteRobot)
    }

    private fun createAndExpendJws(testInfo:TestInfo, jwsName: String, remoteRobot:RemoteRobot, filet: List<Triple<String, String, String>> ){
        injectEmptyJobList(testInfo)
        utilObject.callCreateJWSFromContextMenu(fixtureStack,closableFixtureCollector,remoteRobot)
        addJesWorkingSetSubDialog.fillAddJobFilter(connectionName, jwsName, filet, fixtureStack,remoteRobot)
        addJesWorkingSetSubDialog.okButton.click()
        utilObject.decompressWsIfCompressed(jwsName, filterAllAndZos.first, fixtureStack, remoteRobot)
    }
}
