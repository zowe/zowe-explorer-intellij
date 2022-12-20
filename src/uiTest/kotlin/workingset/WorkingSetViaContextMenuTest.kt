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
import com.intellij.remoterobot.fixtures.ActionButtonFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration


/**
 * Tests creating, editing and deleting working sets and masks from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetViaContextMenuTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add Working Set Dialog", "Edit Working Set Dialog", "Create Mask Dialog")

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
    fun testAddWorkingSetWithoutConnectionViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addConnectionDialog(fixtureStack) {
                addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    /**
     * Tests to add new empty working set with very long name, checks that correct message is returned.
     */
    @Test
    @Order(2)
    fun testAddEmptyWorkingSetWithVeryLongNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName: String = "A".repeat(200)
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    /**
     * Tests to add new working set with one valid mask.
     */
    @Test
    @Order(3)
    fun testAddWorkingSetWithOneValidMaskViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        val mask = Pair("$ZOS_USERID.*", "z/OS")
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, mask)
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    /**
     * Tests to add new working set with several valid z/OS masks, opens masks in explorer.
     */
    @Test
    @Order(4)
    fun testAddWorkingSetWithValidZOSMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        //todo allocate dataset with 44 length when 'Allocate Dataset Dialog' implemented

        validZOSMasks.forEach {
            masks.add(Pair(it, "z/OS"))
        }

        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, masks)
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
        validZOSMasks.forEach { openWSOpenMaskInExplorer(wsName, it, projectName, fixtureStack, remoteRobot) }
    }

    /**
     * Tests to add new working set with several valid USS masks, opens masks in explorer.
     */
    @Test
    @Order(5)
    fun testAddWorkingSetWithValidUSSMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS3"
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        validUSSMasks.forEach {
            masks.add(Pair(it, "USS"))
        }

        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, masks)
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
        validUSSMasks.forEach { openWSOpenMaskInExplorer(wsName, it, projectName, fixtureStack, remoteRobot) }
    }


    /**
     * Tests to add new working set with invalid masks, checks that correct messages are returned.
     */
    @Test
    @Order(6)
    fun testAddWorkingSetWithInvalidMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS4"
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
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
                    Thread.sleep(3000)
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
        }
    }

    /**
     * Tests to add working set with the same masks, checks that correct message is returned.
     */
    @Test
    @Order(7)
    fun testAddWorkingSetWithTheSameMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS4"
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
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
        }
    }

    /**
     * Tests to edit working set by adding one mask, checks that ws is refreshed in explorer, opens new mask.
     */
    @Test
    @Order(8)
    fun testEditWorkingSetAddOneMaskViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        closeMaskInExplorer("$ZOS_USERID.*", projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                addMask(Pair("/u/$ZOS_USERID", "USS"))
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        openMaskInExplorer("/u/$ZOS_USERID", "", projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by deleting several masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    @Order(9)
    fun testEditWorkingSetDeleteMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks = listOf("$ZOS_USERID.*", "Q.*", ZOS_USERID)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                deleteMasks(masks)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        masks.forEach { checkItemWasDeletedWSRefreshed(it, projectName, fixtureStack, remoteRobot) }
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by deleting all masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    @Order(10)
    fun testEditWorkingSetDeleteAllMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val deletedMasks = listOf("$ZOS_USERID.**", "$ZOS_USERID.@#%", "$ZOS_USERID.@#%.*", "WWW.*", maskWithLength44)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                deleteAllMasks()
                clickButton("OK")
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        deletedMasks.forEach { checkItemWasDeletedWSRefreshed(it, projectName, fixtureStack, remoteRobot) }
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by changing connection to invalid, checks that correct message is returned.
     */
    @Test
    @Order(11)
    fun testEditWorkingSetChangeConnectionToInvalidViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newConnectionName = "invalid connection"
        createConnection(projectName, fixtureStack, closableFixtureCollector, newConnectionName, false, remoteRobot)
        val wsName = "WS1"
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                changeConnection(newConnectionName)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
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
    fun testEditWorkingSetChangeConnectionToValidViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newConnectionName = "new $connectionName"
        createConnection(projectName, fixtureStack, closableFixtureCollector, newConnectionName, true, remoteRobot)
        val wsName = "WS1"
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                changeConnection(newConnectionName)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        checkItemWasDeletedWSRefreshed("Invalid URL port: \"104431\"", projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit working set by renaming it, checks that ws is refreshed in explorer.
     */
    @Test
    @Order(13)
    fun testEditWorkingSetRenameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newWorkingSetName = "new ws name"
        val oldWorkingSetName = "WS1"
        val alreadyExistsWorkingSetName = "WS2"
        openOrCloseWorkingSetInExplorer(oldWorkingSetName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(oldWorkingSetName, fixtureStack, closableFixtureCollector)
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
        }
        checkItemWasDeletedWSRefreshed(oldWorkingSetName, projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(newWorkingSetName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete working set, checks that explorer info is refreshed.
     */
    @Test
    @Order(14)
    fun testDeleteWorkingSetViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        ideFrameImpl(projectName, fixtureStack) {
            deleteWSFromContextMenu(wsName)
            clickButton("Yes")
        }
        checkItemWasDeletedWSRefreshed(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to create invalid masks, checks that correct messages are returned.
     */
    @Test
    @Order(15)
    fun testCreateInvalidMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        ideFrameImpl(projectName, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                maskMessageMap.forEach {
                    createMask(Pair(it.key, "z/OS"))
                    Thread.sleep(3000)
                    if (button("OK").isEnabled()) {
                        clickButton("OK")
                    }
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                        it.value
                    )
                    assertFalse(button("OK").isEnabled())
                }
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
    }

    /**
     * Tests to create valid USS and z/OS masks from context menu.
     */
    @Test
    @Order(16)
    fun testCreateValidMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        validZOSMasks.forEach {
            createMaskFromContextMenu(wsName, it, "z/OS", remoteRobot)
            openWSOpenMaskInExplorer(wsName, it, projectName, fixtureStack, remoteRobot)
        }
        validUSSMasks.forEach {
            createMaskFromContextMenu(wsName, it, "USS", remoteRobot)
        }
    }

    /**
     * Tests to create already exists mask in working set, checks that correct message is returned.
     */
    @Test
    @Order(17)
    fun testCreateAlreadyExistsMaskViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        val maskName = "$ZOS_USERID.*"
        ideFrameImpl(projectName, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair(maskName, "z/OS"))
                assertFalse(button("OK").isEnabled())
                val message = find<HeavyWeightWindowFixture>(
                    byXpath("//div[@class='HeavyWeightWindow']"),
                    Duration.ofSeconds(30)
                ).findAllText()
                (message[0].text + message[1].text).shouldContain("You must provide unique mask in working set. Working Set \"$wsName\" already has mask - $maskName")
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
    }

    /**
     * Tests to rename mask, checks that info is refreshed in explorer.
     */
    @Test
    @Order(18)
    fun testRenameMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "new ws name"
        renameMaskFromContextMenu(wsName, "$ZOS_USERID.*", "$ZOS_USERID.**", true, "Rename Dataset Mask", remoteRobot)
        renameMaskFromContextMenu(wsName, "/u/$ZOS_USERID", "/etc/ssh", true, "Rename USS Mask", remoteRobot)
        openWSOpenMaskInExplorer(wsName, "$ZOS_USERID.**", projectName, fixtureStack, remoteRobot)
        openWSOpenMaskInExplorer(wsName, "/etc/ssh", projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        checkItemWasDeletedWSRefreshed("$ZOS_USERID.*", projectName, fixtureStack, remoteRobot)
        checkItemWasDeletedWSRefreshed("/u/$ZOS_USERID", projectName, fixtureStack, remoteRobot)
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to rename mask to already exists, checks that correct message is returned.
     */
    @Test
    @Order(19)
    fun testRenameMaskToAlreadyExistsViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        renameMaskFromContextMenu(wsName, "$ZOS_USERID.*", "$ZOS_USERID.**", false, "Rename Dataset Mask", remoteRobot)
        renameMaskFromContextMenu(wsName, "/u", "/etc/ssh", false, "Rename USS Mask", remoteRobot)
    }

    /**
     * Tests to delete masks, checks that ws is refreshed in explorer and masks were deleted.
     */
    @Test
    @Order(20)
    fun testDeleteMaskViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "new ws name"
        deleteMaskFromContextMenu(wsName, "$ZOS_USERID.**", true, remoteRobot)
        deleteMaskFromContextMenu(wsName, "/etc/ssh", false, remoteRobot)

    }

    /**
     * Deletes mask from working set via context menu. Checks that info is refreshed in explorer.
     */
    private fun deleteMaskFromContextMenu(
        wsName: String,
        maskName: String,
        isZOSMaskType: Boolean,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val dialogTitle = if (isZOSMaskType) {
            "Deletion of DS Mask $maskName"
        } else {
            "Deletion of Uss Path Root $maskName"
        }
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(maskName).rightClick()
            }
            actionMenuItem(remoteRobot, "Delete").click()
            dialog(dialogTitle) {
                clickButton("Yes")
            }
            checkItemWasDeletedWSRefreshed(maskName, projectName, fixtureStack, remoteRobot)
            explorer {
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
            }
        }
    }

    /**
     * Creates mask in working set via context menu.
     */
    private fun createMaskFromContextMenu(
        wsName: String,
        maskName: String,
        maskType: String,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                createMask(wsName, fixtureStack, closableFixtureCollector)
                createMaskDialog(fixtureStack) {
                    createMask(Pair(maskName, maskType))
                    Thread.sleep(3000)
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
            }
        }

    /**
     * Renames mask in working set via context menu.
     */
    private fun renameMaskFromContextMenu(
        wsName: String,
        oldMaskName: String,
        newMaskName: String,
        isNewMaskUnique: Boolean,
        dialogTitle: String,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    fileExplorer.click()
                    find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
                    Thread.sleep(3000)
                    find<ComponentFixture>(viewTree).findText(oldMaskName).rightClick()
                }
                actionMenuItem(remoteRobot, "Edit").click()
                dialog(dialogTitle) {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = newMaskName
                }
                if (isNewMaskUnique) {
                    clickButton("OK")
                } else {
                    assertFalse(button("OK").isEnabled())
                    val message = find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findAllText()
                    (message[0].text + message[1].text).shouldContain("You must provide unique mask in working set. Working Set \"$wsName\" already has mask - $newMaskName")
                    clickButton("Cancel")
                }
                explorer {
                    find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
                }
            }
        }
}
