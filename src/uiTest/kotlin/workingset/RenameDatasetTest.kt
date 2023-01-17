/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package workingset

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests creating, editing and deleting working sets and masks from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class RenameDatasetTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Allocate DataSet Dialog", "Allocate Member Dialog")

    private val projectName = "untitled"
    private val connectionName = "con1"
    private val wsName = "WS name"
    private val errorHeader = "Error in plugin For Mainframe"
    private val errorType = "Unable to rename"

    private val pdsName = "$ZOS_USERID.UI.TEST"
    private val memberName = "TESTM"
    private val memberFinalName = "TESTMF"
    private val anotherMemberName = "TESTMA"

    private val dsName = "$ZOS_USERID.UI.TESTD"
    private val dsFinalName = "$ZOS_USERID.UI.TESTDF"
    private val anotherDsName = "$ZOS_USERID.UI.TESTA"

    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(projectName, fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        createWsWithoutMask(projectName, wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        allocatePDSAndCreateMask(
            wsName,
            pdsName,
            projectName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot,
            "$ZOS_USERID.UI.TEST*",
            directory = 2
        )
        allocateDataSet(wsName, dsName, projectName, fixtureStack, remoteRobot)
        allocateDataSet(wsName, anotherDsName, projectName, fixtureStack, remoteRobot)
        allocateMemberForPDS(pdsName, memberName, projectName, fixtureStack, remoteRobot)
        allocateMemberForPDS(pdsName, anotherMemberName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        deleteDataset(pdsName, projectName, fixtureStack, remoteRobot)
        deleteDataset(anotherDsName, projectName, fixtureStack, remoteRobot)
        deleteDataset(dsFinalName, projectName, fixtureStack, remoteRobot)
        clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            close()
        }
    }

    /**
     * Closes all unclosed closable fixtures that we want to close.
     */
    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) {
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests renaming member when valid member name is provided.
     */
    @Test
    @Order(1)
    fun testRenameMemberWithCorrectNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(pdsName).doubleClick()
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = memberFinalName
            }
            clickButton("OK")
        }
    }

    /**
     * Tests renaming member to name of another member in the same PDS and validates error pop-up notification.
     */
    @Test
    @Order(2)
    fun testRenameMemberWithNameOfAnotherMemberViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val errorDetail = "Member already exists"

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = anotherMemberName
            }
            clickButton("OK")
            Thread.sleep(3000)
            checkErrorNotification(errorHeader, errorType, errorDetail, projectName, fixtureStack, remoteRobot)
        }
    }

    /**
     * Tests renaming member to the same name and validates error pop-up notification.
     */
    @Test
    @Order(3)
    fun testRenameMemberWithTheSameNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val errorDetail = "Member in use"

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = memberFinalName
            }
            clickButton("OK")
            Thread.sleep(3000)
            checkErrorNotification(errorHeader, errorType, errorDetail, projectName, fixtureStack, remoteRobot)
        }
    }

    /**
     * Tests renaming member to very long and validates error notification.
     */
    @Test
    @Order(4)
    fun testRenameMemberWithTooLongNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "123456789"
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                MEMBER_NAME_LENGTH_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming member to invalid name and validates error notification.
     */
    @Test
    @Order(5)
    fun testRenameMemberWithInvalidNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "@*"
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                INVALID_MEMBER_NAME_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming member to name with the invalid first symbol and validates error notification.
     */
    @Test
    @Order(6)
    fun testRenameMemberWithInvalidFirstSymbolViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "**"
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                INVALID_MEMBER_NAME_BEGINNING_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming member to empty name and validates error notification.
     */
    @Test
    @Order(7)
    fun testRenameMemberWithEmptyNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = ""
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                MEMBER_EMPTY_NAME_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming DataSet when valid member name is provided.
     */
    @Test
    @Order(8)
    fun testRenameDataSetWithCorrectNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(dsName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = dsFinalName
            }
            clickButton("OK")
        }
    }

    /**
     * Tests renaming DataSet to name of another DataSet and validates error pop-up notification.
     */
    @Test
    @Order(9)
    fun testRenameDatasetWithNameOfAnotherDatasetViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val errorDetail = "data set rename failed"

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = anotherDsName
            }
            clickButton("OK")
            Thread.sleep(3000)
            checkErrorNotification(errorHeader, errorType, errorDetail, projectName, fixtureStack, remoteRobot)
        }
    }

    /**
     * Tests renaming DataSet to name with invalid section and validates error notification.
     */
    @Test
    @Order(10)
    fun testRenameDatasetWithInvalidSectionViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = dsFinalName + ".123456789"
            }
            var message = ""
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findAllText().forEach {
                message += it.text
            }
            Thread.sleep(3000)
            clickButton("Cancel")
            if (!message.contains(DATASET_INVALID_SECTION_MESSAGE)) {
                throw Exception("Error message is different from expected")
            }
        }
    }

    /**
     * Tests renaming DataSet to very long name and validates error notification.
     */
    @Test
    @Order(11)
    fun testRenameDatasetWithTooLongNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "A".repeat(45)
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                DATASET_NAME_LENGTH_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming DataSet to empty name and validates error notification.
     */
    @Test
    @Order(12)
    fun testRenameDatasetWithEmptyNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = ""
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                DATASET_EMPTY_NAME_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }
}
