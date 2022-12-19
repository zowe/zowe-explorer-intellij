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
            "$ZOS_USERID.*",
            directory = 2
        )
        allocateMemberForPDS(pdsName, memberName, projectName, fixtureStack, remoteRobot)
        allocateMemberForPDS(pdsName, anotherMemberName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        //clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        //ideFrameImpl(projectName, fixtureStack) {
        //    close()
        //}
    }

    /**
     * Closes all unclosed closable fixtures that we want to close.
     */
    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) {
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests to add new working set without connection, checks that correct message is returned.
     */
    @Test
    @Order(1)
    fun testRenameMemberWithCorrectNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
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
            checkErrorNotification(errorHeader, errorType, errorDetail, remoteRobot)
        }
    }

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
            checkErrorNotification(errorHeader, errorType, errorDetail, remoteRobot)
        }
    }

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

    private fun checkErrorNotification(
        errorHeader: String,
        errorType: String,
        errorDetail: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            var errorMessage = ""
            find<ComponentFixture>(byXpath("//div[@class='LinkLabel']")).click()
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText(errorHeader)
            find<ContainerFixture>(byXpath("//div[@class='JEditorPane']")).findAllText().forEach {
                errorMessage += it.text
            }
            if (!(errorMessage.contains(errorType) && errorMessage.contains(errorDetail))) {
                find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
                throw Exception("Error message is different from expected")
            } else {
                find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
            }
        }
    }
}
