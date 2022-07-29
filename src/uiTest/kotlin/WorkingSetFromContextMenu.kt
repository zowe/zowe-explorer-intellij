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
import auxiliary.components.ActionMenuFixture
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ActionButtonFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JLabelFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith


/**
 * Tests creating working sets and masks.
 */
@Tag("FirstTime")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetFromContextMenu {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add Working Set Dialog")

    private val projectName = "untitled"
    private val connectionName = "valid connection"

    private val maskWithLength44 = "$ZOS_USERID."+"A2345678.".repeat((44-(ZOS_USERID.length+1))/9)+"A".repeat((44-(ZOS_USERID.length+1))%9)
    private val maskWithLength45 = "$ZOS_USERID."+"A2345678.".repeat((45-(ZOS_USERID.length+1))/9)+"A".repeat((45-(ZOS_USERID.length+1))%9)

    private val enterValidDSMaskMessage = "Enter valid dataset mask"
    private val maskMessageMap = mapOf("1$ZOS_USERID.*" to enterValidDSMaskMessage,
        "$ZOS_USERID.{!" to enterValidDSMaskMessage,
        "$ZOS_USERID.A23456789.*" to "Qualifier must be in 1 to 8 characters",
        "$ZOS_USERID." to enterValidDSMaskMessage,
        maskWithLength45 to "Dataset mask must be no more than 44 characters",
    )

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
         /*   if (dialog("For Mainframe Plugin Privacy Policy and Terms and Conditions").isShowing) {
                clickButton("I Agree")
            }*/
            forMainframe()
        }
        deleteAllConnections(remoteRobot)
        //workaround for bug ijmp-664
        createConnection(connectionName, true, remoteRobot)
        createFirstWorkingSet(remoteRobot)
    }

    /**
     * Closes the project.
     */
    //  @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        deleteAllConnections(remoteRobot)
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

   // @Test
    @Order(1)
    fun testAddWorkingSetWithoutConnectionFromContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).rightClick()
                find<ComponentFixture>(byXpath("//div[@class='MyMenu']"))
                actionMenu(remoteRobot,"New")
                actionMenuItem(remoteRobot,"Working Set")
            }
            addConnectionDialog(fixtureStack) {
                addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName,connectionName)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }


   // @Test
    @Order(2)
    fun testAddEmptyWorkingSetWithVeryLongNameFromContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName: String = "B".repeat(200)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).rightClick()
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).find<ComponentFixture>(
                    byXpath("//div[contains(@text.key, 'group.NewGroup.text')]")
                ).click()
             //  find<ComponentFixture>(byXpath("//div[@class='JEditorPane']")).findText("IDE error occurred").moveMouse()
                //  find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
               // find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).rightClick()
                   // find<ComponentFixture>(byXpath("//div[contains(@text.key, 'group.NewGroup.text') and @class='ActionMenu']"))
               // find<ComponentFixture>(byXpath("//div[@class='JPanel'][.//div[@class='MyMenu']]"))
              //  actionMenu(remoteRobot,"New")
                actionMenuItem(remoteRobot,"Working Set")
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName,connectionName)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }


    fun testAddWorkingSetWithOneValidMaskFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        val mask = Pair("$ZOS_USERID.*","z/OS")
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName,connectionName,mask)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }


    fun testAddWorkingSetWithValidZOSMasksFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks: ArrayList<Pair<String,String>>  = ArrayList()
        //todo allocate dataset with 44 length
        val masksNames = listOf("$ZOS_USERID.*","$ZOS_USERID.**","$ZOS_USERID.@#%","$ZOS_USERID.@#%.*","Q.*","WWW.*",maskWithLength44,
            ZOS_USERID
        )
        masksNames.forEach {
            masks.add(Pair(it,"z/OS")) }

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName,connectionName,masks)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
        //todo open masks in explorer
    }


    fun testAddWorkingSetWithValidUSSMasksFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS3"
        val masks: ArrayList<Pair<String,String>>  = ArrayList()
        val masksNames = listOf("/u","/uuu","/etc/ssh","/u/$ZOS_USERID")
        masksNames.forEach {
            masks.add(Pair(it,"USS")) }

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName,connectionName,masks)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
        //todo open masks in explorer
    }



    fun testAddWorkingSetWithInvalidMasksFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        //todo add mask *.* when bug is fixed
        val wsName = "WS4"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName,connectionName)
                maskMessageMap.forEach{
                    addMask(Pair(it.key,"z/OS"))
                    if (button("OK").isEnabled()){
                        clickButton("OK")
                    } else {
                        findText("OK").moveMouse()
                    }
                    if (it.key.length<45){
                        findText(it.key).moveMouse()
                    } else {
                        findText("${it.key.substring(0,42)}...").moveMouse()
                    }
                    Thread.sleep(5000)
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow'][.//div[@class='Header']]")).findText(it.value)
                    assertFalse(button("OK").isEnabled())
                    findText(it.key).click()
                    clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))
                }

                addMask(Pair("$ZOS_USERID.*","z/OS"))
                addMask(Pair("$ZOS_USERID.*","z/OS"))
                assertFalse(button("OK").isEnabled())
                find<JLabelFixture>(byXpath("//div[@accessiblename='You cannot add several identical masks to table' and @class='JLabel']"))

                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }


    private fun createConnection(connectionName:String, isValidConnection: Boolean, remoteRobot: RemoteRobot) = with(remoteRobot){
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
                    if (isValidConnection) {
                        addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                    } else {
                        addConnection(connectionName, "${CONNECTION_URL}1", ZOS_USERID, ZOS_PWD, true)
                    }
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                if (isValidConnection.not()){
                    errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                        clickButton("Yes")
                    }
                    closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
                }
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    private fun deleteAllConnections(remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    conTab.click()
                    deleteAllItems("Connections")
                }
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    private fun createFirstWorkingSet(remoteRobot:RemoteRobot) = with(remoteRobot){
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    addWS(closableFixtureCollector, fixtureStack)
                }
                addWorkingSetDialog(fixtureStack) {
                    addWorkingSet("testWS",connectionName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }
}
