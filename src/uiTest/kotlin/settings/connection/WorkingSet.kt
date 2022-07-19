/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package settings.connection

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith


/**
 * Tests creating working sets and masks.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSet {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private val wantToClose = listOf(
        "Settings Dialog",
        "Add Working Set Dialog"
    )
    private val projectName = "untitled"
    private val connectionName = "valid connection"


    /**
     * Opens the project and Explorer, create valid connection.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            open(projectName)
        }
        Thread.sleep(10000)

        ideFrameImpl(projectName, fixtureStack) {
            if (dialog("For Mainframe Plugin Privacy Policy and Terms and Conditions").isShowing) {
                clickButton("I Agree")
            }
            forMainframe()
        }
    }

    /**
     * Closes the project.
     */
 //   @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            close()
        }
    }

    /**
     * Closes all unclosed closable fixtures that we want to close.
     */
 //   @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) {
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    @Test
    @Order(1)
    fun testAddWorkingSetWithoutConnection(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    add(closableFixtureCollector, fixtureStack)
                }
                addWorkingSetDialog(fixtureStack) {
                    addWorkingSet("WS1","")
                    clickButton("OK")
                    comboBox("Specify connection").click()
                   // TODO check "You must provide a connection"
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    @Test
    @Order(2)
    fun testAddEmptyWorkingSetWithVeryLongName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createValidConnection(remoteRobot)
        val wsName: String = "A".repeat(200)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    add(closableFixtureCollector, fixtureStack)
                }
                addWorkingSetDialog(fixtureStack) {
                    addWorkingSet(wsName,connectionName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    @Test
    @Order(3)
    fun testAddWorkingSetWithOneValidMask(remoteRobot: RemoteRobot) = with(remoteRobot) {
     // createValidConnection(remoteRobot)
      val wsName = "WS1"
        val mask = Pair("$ZOS_USERID.*","z/OS")
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    add(closableFixtureCollector, fixtureStack)
                }
                addWorkingSetDialog(fixtureStack) {
                    addWorkingSet(wsName,connectionName,mask)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    @Test
    @Order(4)
    fun testAddWorkingSetWithValidMasks(remoteRobot: RemoteRobot) = with(remoteRobot) {
       val wsName = "WS2"
       val masks: ArrayList<Pair<String,String>>  = ArrayList()
       //todo allocate dataset with 44 length
       val maskWithLength44 = "$ZOS_USERID."+"A2345678.".repeat((44-(ZOS_USERID.length+1))/9)+"A".repeat((44-(ZOS_USERID.length+1))%9)
       val masksNames = listOf("$ZOS_USERID.*","$ZOS_USERID.**","$ZOS_USERID.@#%","$ZOS_USERID.@#%.*","Q.*","WWW.*",maskWithLength44,
           ZOS_USERID
       )
       masksNames.forEach {
           masks.add(Pair(it,"z/OS")) }

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    add(closableFixtureCollector, fixtureStack)
                }
                addWorkingSetDialog(fixtureStack) {
                    addWorkingSet(wsName,connectionName,masks)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
       //todo open masks in explorer
    }

    private fun createValidConnection(remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    conTab.click()
                    add(closableFixtureCollector, fixtureStack)
                }
                addConnectionDialog(fixtureStack) {
                    addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }
}
