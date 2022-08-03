/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package settings.workingset

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JLabelFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith


/**
 * Tests creating working sets and masks.
 */
//@Tag("FirstTime")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetFromContextMenuTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add Working Set Dialog")

    private val projectName = "untitled"
    private val connectionName = "valid connection"


    /**
     * Opens the project and Explorer, clear test environment..
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
    }

    /**
     * Closes the project  and clear test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
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

    @Test
    @Order(1)
    fun testAddWorkingSetWithoutConnectionFromContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).rightClick()
                find<ComponentFixture>(byXpath("//div[@class='MyMenu']"))
                actionMenu(remoteRobot, "New")
                actionMenuItem(remoteRobot, "Working Set")
            }
            addConnectionDialog(fixtureStack) {
                addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }


}
