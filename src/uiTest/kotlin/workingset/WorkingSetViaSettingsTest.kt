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
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ActionButtonFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration


/**
 * Tests creating working sets and masks via settings.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
@Tag("FirstTime")
class WorkingSetViaSettingsTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Settings Dialog", "Add Working Set Dialog", "Edit Working Set Dialog"
    )
    private val projectName = "untitled"
    private val connectionName = "valid connection"


    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
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

    /**
     * Tests to add new working set without connection, checks that correct message is returned.
     */
    @Test
    @Order(1)
    fun testAddWorkingSetWithoutConnectionViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                    addWorkingSet("WS1", "")
                    clickButton("OK")
                    comboBox("Specify connection").click()
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText("You must provide a connection")
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to add new empty working set with very long name, checks that correct message is returned.
     */
    @Test
    @Order(2)
    fun testAddEmptyWorkingSetWithVeryLongNameViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createConnection(projectName, fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        val wsName: String = "A".repeat(200)
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
                    addWorkingSet(wsName, connectionName)
                    clickButton("OK")
                    Thread.sleep(5000)
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                        EMPTY_DATASET_MESSAGE
                    )
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to add new working set with one valid mask.
     */
    @Test
    @Order(3)
    fun testAddWorkingSetWithOneValidMaskViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        val mask = Pair("$ZOS_USERID.*", "z/OS")
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
                    addWorkingSet(wsName, connectionName, mask)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to add new working set with several valid z/OS masks, opens masks in explorer.
     */
    @Test
    @Order(4)
    fun testAddWorkingSetWithValidZOSMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        //todo allocate dataset with 44 length when 'Allocate Dataset Dialog' implemented

        validZOSMasks.forEach {
            masks.add(Pair(it, "z/OS"))
        }

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
                    addWorkingSet(wsName, connectionName, masks)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        validZOSMasks.forEach { openWSOpenMaskInExplorer(wsName, it, projectName, fixtureStack, remoteRobot) }
    }

    /**
     * Tests to add new working set with several valid USS masks, opens masks in explorer.
     */
    @Test
    @Order(5)
    fun testAddWorkingSetWithValidUSSMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS3"
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        validUSSMasks.forEach {
            masks.add(Pair(it, "USS"))
        }

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
                    addWorkingSet(wsName, connectionName, masks)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        validUSSMasks.forEach { openWSOpenMaskInExplorer(wsName, it, projectName, fixtureStack, remoteRobot) }
    }


    /**
     * Tests to add new working set with invalid masks, checks that correct messages are returned.
     */
    @Test
    @Order(6)
    fun testAddWorkingSetWithInvalidMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS4"
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
                    addWorkingSet(wsName, connectionName)
                    maskMessageMap.forEach {
                        addMask(Pair(it.key, "z/OS"))
                        if (button("OK").isEnabled()) {
                            clickButton("OK")
                        } else {
                            findText("OK").moveMouse()
                        }
                        if (it.key.length < 49) {
                            findText(it.key).moveMouse()
                        } else {
                            findText("${it.key.substring(0, 46)}...").moveMouse()
                        }
                        Thread.sleep(5000)
                        find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow'][.//div[@class='Header']]")).findText(
                            it.value
                        )
                        assertFalse(button("OK").isEnabled())
                        findText(it.key).click()
                        clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))
                    }
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to add working set with the same masks, checks that correct message is returned.
     */
    @Test
    @Order(7)
    fun testAddWorkingSetWithTheSameMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS4"
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
                    addWorkingSet(wsName, connectionName)
                    addMask(Pair("$ZOS_USERID.*", "z/OS"))
                    addMask(Pair("$ZOS_USERID.*", "z/OS"))
                    clickButton("OK")
                    find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findText(IDENTICAL_MASKS_MESSAGE)
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to edit working set by adding one mask, checks that ws is refreshed in explorer, opens new mask.
     */
    @Test
    @Order(8)
    fun testEditWorkingSetAddOneMaskViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        closeMaskInExplorer("$ZOS_USERID.*", projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    editWorkingSet(wsName, closableFixtureCollector, fixtureStack)
                }
                editWorkingSetDialog(fixtureStack) {
                    addMask(Pair("/u/$ZOS_USERID", "USS"))
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        openMaskInExplorer("/u/$ZOS_USERID", "", projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by deleting several masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    @Order(9)
    fun testEditWorkingSetDeleteMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks = listOf("$ZOS_USERID.*", "Q.*", ZOS_USERID)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    editWorkingSet(wsName, closableFixtureCollector, fixtureStack)
                }
                editWorkingSetDialog(fixtureStack) {
                    deleteMasks(masks)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        masks.forEach { checkItemWasDeletedWSRefreshed(it, projectName, fixtureStack, remoteRobot) }
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by deleting all masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    @Order(10)
    fun testEditWorkingSetDeleteAllMasksViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val deletedMasks = listOf("$ZOS_USERID.**", "$ZOS_USERID.@#%", "$ZOS_USERID.@#%.*", "WWW.*", maskWithLength44)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    editWorkingSet(wsName, closableFixtureCollector, fixtureStack)
                }
                editWorkingSetDialog(fixtureStack) {
                    deleteAllMasks()
                    clickButton("OK")
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText("You are going to create a Working Set that doesn't fetch anything")
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        deletedMasks.forEach { checkItemWasDeletedWSRefreshed(it, projectName, fixtureStack, remoteRobot) }
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by changing connection to invalid, checks that correct message is returned.
     */
    @Test
    @Order(11)
    fun testEditWorkingSetChangeConnectionToInvalidViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newConnectionName = "invalid connection"
        val wsName = "WS1"
        createConnection(projectName, fixtureStack, closableFixtureCollector, newConnectionName, false, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    editWorkingSet(wsName, closableFixtureCollector, fixtureStack)
                }
                editWorkingSetDialog(fixtureStack) {
                    changeConnection(newConnectionName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        findAll<ComponentFixture>(byXpath("//div[@class='MyComponent'][.//div[@accessiblename='Invalid URL port: \"104431\"' and @class='JEditorPane']]")).forEach {
            it.click()
            findAll<ActionButtonFixture>(
                byXpath("//div[@class='ActionButton' and @myicon= 'close.svg']")
            ).first().click()
        }
        openMaskInExplorer("$ZOS_USERID.*", "Invalid URL port: \"104431\"", projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by changing connection from invalid to valid, checks that ws is refreshed in explorer and error message disappeared.
     */
    @Test
    @Order(12)
    fun testEditWorkingSetChangeConnectionToValidViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newConnectionName = "new $connectionName"
        createConnection(projectName, fixtureStack, closableFixtureCollector, newConnectionName, true, remoteRobot)
        val wsName = "WS1"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    editWorkingSet(wsName, closableFixtureCollector, fixtureStack)
                }
                editWorkingSetDialog(fixtureStack) {
                    changeConnection(newConnectionName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        checkItemWasDeletedWSRefreshed("Invalid URL port: \"104431\"", projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by renaming it, checks that ws is refreshed in explorer.
     */
    @Test
    @Order(13)
    fun testEditWorkingSetRenameViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newWorkingSetName = "new ws name"
        val oldWorkingSetName = "WS1"
        val alreadyExistsWorkingSetName = "WS2"
        openOrCloseWorkingSetInExplorer(oldWorkingSetName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    editWorkingSet(oldWorkingSetName, closableFixtureCollector, fixtureStack)
                }
                editWorkingSetDialog(fixtureStack) {
                    renameWorkingSet(alreadyExistsWorkingSetName)
                    val message = find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findAllText()
                    (message[0].text + message[1].text).shouldContain("You must provide unique working set name. Working Set $alreadyExistsWorkingSetName already exists.")
                    renameWorkingSet(newWorkingSetName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        checkItemWasDeletedWSRefreshed(oldWorkingSetName, projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(newWorkingSetName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete working set, checks that explorer info is refreshed.
     */
    @Test
    @Order(14)
    fun testDeleteWorkingSetViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    deleteItem(wsName)
                }
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        checkItemWasDeletedWSRefreshed(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete all working sets, checks that explorer info is refreshed.
     */
    @Test
    @Order(15)
    fun testDeleteAllWorkingSetsViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    workingSetsTab.click()
                    deleteAllItems()
                }
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
            find<ComponentFixture>(viewTree).findText("Nothing to show")
        }
    }
}
