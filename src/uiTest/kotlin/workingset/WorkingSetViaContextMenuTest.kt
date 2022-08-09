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
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration


/**
 * Tests creating, editing and deleting working sets and masks from context menu.
 */
//@Tag("FirstTime")
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
     * Opens the project and Explorer, clear test environment.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
    }

    /**
     * Closes the project  and clear test environment.
     */
    //  @AfterAll
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
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).hasText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

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

    @Test
    @Order(4)
    fun testAddWorkingSetWithValidZOSMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        //todo allocate dataset with 44 length

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
        //todo open masks in explorer
    }

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
        //todo open masks in explorer
    }


    @Test
    @Order(6)
    fun testAddWorkingSetWithInvalidMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        //todo add mask *.* when bug is fixed
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
                ).hasText(IDENTICAL_MASKS_MESSAGE)
                assertFalse(button("OK").isEnabled())
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    @Test
    @Order(8)
    fun testEditWorkingSetAddOneMaskViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                addMask(Pair("/u/$ZOS_USERID", "USS"))
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)

        }
        //todo check in explorer, ws refreshed
    }

    @Test
    @Order(9)
    fun testEditWorkingSetDeleteMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks = listOf("$ZOS_USERID.*", "Q.*", ZOS_USERID)
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                deleteMasks(masks)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        //todo check in explorer, ws refreshed
    }

    @Test
    @Order(10)
    fun testEditWorkingSetDeleteAllMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(wsName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                deleteAllMasks()
                clickButton("OK")
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText("You are going to create a Working Set that doesn't fetch anything")
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        //todo check in explorer, ws refreshed
    }

    @Test
    @Order(11)
    fun testEditWorkingSetChangeConnectionToInvalidViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newConnectionName = "invalid connection"
        createConnection(projectName, fixtureStack, closableFixtureCollector, newConnectionName, false, remoteRobot)
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
        //todo check in explorer, ws refreshed
    }

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
        //todo check in explorer, ws refreshed
    }

    @Test
    @Order(13)
    fun testEditWorkingSetRenameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newWorkingSetName = "new ws name"
        val oldWorkingSetName = "WS1"
        val alreadyExistsWorkingSetName = "WS2"
        ideFrameImpl(projectName, fixtureStack) {
            editWSFromContextMenu(oldWorkingSetName, fixtureStack, closableFixtureCollector)
            editWorkingSetDialog(fixtureStack) {
                renameWorkingSet(alreadyExistsWorkingSetName)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).find<ComponentFixture>(
                    byXpath("//div[@class='JEditorPane']")
                )
                    .hasText("You must provide unique working set name. Working Set $alreadyExistsWorkingSetName already || exists.")

                renameWorkingSet(newWorkingSetName)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditWorkingSetDialog.name)
        }
        //todo check in explorer, ws refreshed
    }

    @Test
    @Order(14)
    fun testDeleteWorkingSetViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        ideFrameImpl(projectName, fixtureStack) {
            deleteWSFromContextMenu(wsName)
            clickButton("Yes")
        }
        //todo check in explorer
    }

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

    @Test
    @Order(16)
    fun testCreateValidMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        validZOSMasks.forEach { createMaskFromContextMenu(wsName, it, "z/OS", remoteRobot) }
        validUSSMasks.forEach { createMaskFromContextMenu(wsName, it, "USS", remoteRobot) }
    }

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
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).hasText(
                    "You must provide unique mask in working set. Working Set \"$wsName\" || already has mask - $maskName"
                )
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(CreateMaskDialog.name)
        }
    }

    @Test
    @Order(18)
    fun testRenameMasksViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "new ws name"
        renameMaskFromContextMenu(wsName, "$ZOS_USERID.*", "$ZOS_USERID.**", true, "Rename Dataset Mask", remoteRobot)
        renameMaskFromContextMenu(wsName, "/u/$ZOS_USERID", "/etc/ssh", true, "Rename Directory", remoteRobot)
    }

    @Test
    @Order(19)
    fun testRenameMaskToAlreadyExistsViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        renameMaskFromContextMenu(wsName, "$ZOS_USERID.*", "$ZOS_USERID.**", false, "Rename Dataset Mask", remoteRobot)
        renameMaskFromContextMenu(wsName, "/u", "/etc/ssh", false, "Rename Directory", remoteRobot)
    }

    @Test
    @Order(20)
    fun testDeleteMaskViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "new ws name"
        deleteMaskFromContextMenu(wsName, "$ZOS_USERID.**", true, remoteRobot)
        deleteMaskFromContextMenu(wsName, "/etc/ssh", false, remoteRobot)

    }

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
                find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).findText(
                    wsName
                )
                    .doubleClick()
                Thread.sleep(3000)
                find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).findText(
                    maskName
                ).rightClick()
            }
            actionMenuItem(remoteRobot, "Delete").click()
            dialog(dialogTitle) {
                clickButton("Yes")
            }
            //todo check that mask was deleted, ws was refreshed
            explorer {
                find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).findText(
                    wsName
                )
                    .doubleClick()
            }
        }
    }

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
                    find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).findText(
                        wsName
                    )
                        .doubleClick()
                    Thread.sleep(3000)
                    find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).findText(
                        oldMaskName
                    ).rightClick()
                }
                actionMenuItem(remoteRobot, "Edit").click()
                dialog(dialogTitle) {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = newMaskName
                }
                if (isNewMaskUnique) {
                    clickButton("OK")
                } else {
                    assertFalse(button("OK").isEnabled())
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).hasText(
                        "You must provide unique mask in working set. Working Set \"$wsName\" || already has mask - $newMaskName"
                    )
                    clickButton("Cancel")
                }
                explorer {
                    find<ComponentFixture>(byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")).findText(
                        wsName
                    )
                        .doubleClick()
                }
            }
        }
}
