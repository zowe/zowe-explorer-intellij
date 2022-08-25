/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package auxiliary

import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.waitFor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.string.shouldContain
import java.time.Duration

//Change ZOS_USERID, ZOS_PWD, CONNECTION_URL with valid values before UI tests execution
const val ZOS_USERID = "changeme"
const val ZOS_PWD = "changeme"
const val CONNECTION_URL = "changeme"

const val ENTER_VALID_DS_MASK_MESSAGE = "Enter valid dataset mask"
const val IDENTICAL_MASKS_MESSAGE = "You cannot add several identical masks to table"
const val EMPTY_DATASET_MESSAGE = "You are going to create a Working Set that doesn't fetch anything"

val maskWithLength44 =
    "$ZOS_USERID." + "A2345678.".repeat((44 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((44 - (ZOS_USERID.length + 1)) % 9)
val maskWithLength45 =
    "$ZOS_USERID." + "A2345678.".repeat((45 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((45 - (ZOS_USERID.length + 1)) % 9)

//todo add mask *.* when bug is fixed
val maskMessageMap = mapOf(
    "1$ZOS_USERID.*" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.{!" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.A23456789.*" to "Qualifier must be in 1 to 8 characters",
    "$ZOS_USERID." to ENTER_VALID_DS_MASK_MESSAGE,
    maskWithLength45 to "Dataset mask must be no more than 44 characters",
    "$ZOS_USERID.***" to "Invalid asterisks in the qualifier"
)

val validZOSMasks = listOf(
    "$ZOS_USERID.*", "$ZOS_USERID.**", "$ZOS_USERID.@#%", "$ZOS_USERID.@#%.*", "Q.*", "WWW.*", maskWithLength44,
    ZOS_USERID
)

val validUSSMasks = listOf("/u", "/uuu", "/etc/ssh", "/u/$ZOS_USERID")

val viewTree = byXpath("//div[@class='JBViewport'][.//div[@class='DnDAwareTree']]")

/**
 * Waits 60 seconds for the button to be enabled and then clicks on it.
 */
fun CommonContainerFixture.clickButton(text: String) {
    val button = button(text)
    waitFor(Duration.ofSeconds(60)) {
        button.isEnabled()
    }
    button.click()
}

/**
 * Waits a specific amount of time for the button to be enabled and then clicks on it.
 */
fun CommonContainerFixture.clickButton(locator: Locator, duration: Duration = Duration.ofSeconds(60)) {
    val button = button(locator)
    waitFor(duration) {
        button.isEnabled()
    }
    button.click()
}

/**
 * Waits 60 seconds for the action button to be enabled and then clicks on it.
 */
fun CommonContainerFixture.clickActionButton(locator: Locator) {
    val button = actionButton(locator)
    waitFor(Duration.ofSeconds(60)) {
        button.isEnabled()
    }
    button.click()
}

/**
 * Creates a working set via context menu from explorer.
 */
fun ContainerFixture.createWSFromContextMenu(
    fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, "New").click()

    //workaround when an action menu contains more than 2 action menu items, and you need to choose the 3d item
    runJs(
        """
            const point = new java.awt.Point(${locationOnScreen.x}, ${locationOnScreen.y});
            robot.moveMouse(component, point);
        """
    )
    actionMenuItem(remoteRobot, "Working Set").click()
    closableFixtureCollector.add(AddWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Edits a working set via context menu from explorer.
 */
fun ContainerFixture.editWSFromContextMenu(
    wsName: String, fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, "Edit").click()
    closableFixtureCollector.add(EditWorkingSetDialog.xPath(), fixtureStack)
}

/**
 * Creates a mask in the working set via context menu from explorer.
 */
fun ContainerFixture.createMask(
    wsName: String, fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector
) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenu(remoteRobot, "New").click()
    actionMenuItem(remoteRobot, "Mask").click()
    closableFixtureCollector.add(CreateMaskDialog.xPath(), fixtureStack)
}

/**
 * Deletes a working set via context menu from explorer.
 */
fun ContainerFixture.deleteWSFromContextMenu(wsName: String) {
    explorer {
        fileExplorer.click()
        find<ComponentFixture>(viewTree).findText(wsName)
            .rightClick()
        Thread.sleep(3000)
    }
    actionMenuItem(remoteRobot, "Delete").click()
    find<ComponentFixture>(byXpath("//div[@class='MyDialog' and @title='Deletion of Working Set $wsName']"))
}

/**
 * Steps to create a connection(valid or invalid) from settings .
 */
fun createConnection(
    projectName: String, fixtureStack: MutableList<Locator>, closableFixtureCollector: ClosableFixtureCollector,
    connectionName: String, isValidConnection: Boolean, remoteRobot: RemoteRobot
) = with(remoteRobot) {
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
            if (isValidConnection.not()) {
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

/**
 * Deletes all jobs working sets, working sets and connections. To be used in BeforeAll and AfterAll tests methods.
 */
fun clearEnvironment(
    projectName: String,
    fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector,
    remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            settings(closableFixtureCollector, fixtureStack)
        }
        settingsDialog(fixtureStack) {
            configurableEditor {
                workingSetsTab.click()
                deleteAllItems()
                jobsWorkingSetsTab.click()
                deleteAllItems()
                conTab.click()
                deleteAllItems()
            }
            clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
}

/**
 * Opens a project and an explorer, clears test environment before tests execution.
 */
fun setUpTestEnvironment(
    projectName: String,
    fixtureStack: MutableList<Locator>,
    closableFixtureCollector: ClosableFixtureCollector,
    remoteRobot: RemoteRobot
) = with(remoteRobot) {
    welcomeFrame {
        open(projectName)
    }
    Thread.sleep(10000)

    ideFrameImpl(projectName, fixtureStack) {
        try {
            if (dialog("For Mainframe Plugin Privacy Policy and Terms and Conditions").isShowing) {
                clickButton("I Agree")
            }
        } catch (e: WaitForConditionTimeoutException) {
            e.message.shouldContain("Failed to find 'Dialog' by 'title For Mainframe Plugin Privacy Policy and Terms and Conditions'")
        }
        forMainframe()
    }
    clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
}

/**
 * Opens a mask in the working set in explorer.
 */
fun openWSOpenMaskInExplorer(
    wsName: String, maskName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
                Thread.sleep(20000)
                find<ComponentFixture>(viewTree).findAllText().shouldNotContain("Error")
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
            }
        }
    }

/**
 * Double-clicks on the working set to open or close it in explorer.
 */
fun openOrCloseWorkingSetInExplorer(
    wsName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
            Thread.sleep(3000)
        }
    }
}

/**
 * Double-clicks on the mask in explorer to open it and checks the message if required.
 */
fun openMaskInExplorer(
    maskName: String, expectedError: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
                Thread.sleep(20000)
                if (expectedError.isEmpty().not()) {
                    find<ComponentFixture>(viewTree).findText(expectedError)
                } else {
                    find<ComponentFixture>(viewTree).findAllText().shouldNotContain("Error")
                }
            }
        }
    }

/**
 * Double-clicks on mask to close it in explorer.
 */
fun closeMaskInExplorer(
    maskName: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) =
    with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findText(maskName).doubleClick()
            }
        }
    }

/**
 * Checks that the mask or the working set is not displayed in explorer.
 */
fun checkItemWasDeletedWSRefreshed(
    deletedItem: String, projectName: String,
    fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot
) = with(remoteRobot) {
    ideFrameImpl(projectName, fixtureStack) {
        explorer {
            shouldThrow<NoSuchElementException> {
                find<ComponentFixture>(viewTree).findText(deletedItem)
            }
        }
    }
}

