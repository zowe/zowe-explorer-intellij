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
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.waitFor
import io.kotest.matchers.string.shouldContain
import java.time.Duration

const val ZOS_USERID = "BOL"
const val ZOS_PWD = "xsw23edc"
const val CONNECTION_URL = "https://172.20.2.121:10443"

const val ENTER_VALID_DS_MASK_MESSAGE = "Enter valid dataset mask"
const val IDENTICAL_MASKS_MESSAGE = "You cannot add several identical masks to table"

val maskWithLength44 =
    "$ZOS_USERID." + "A2345678.".repeat((44 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((44 - (ZOS_USERID.length + 1)) % 9)
val maskWithLength45 =
    "$ZOS_USERID." + "A2345678.".repeat((45 - (ZOS_USERID.length + 1)) / 9) + "A".repeat((45 - (ZOS_USERID.length + 1)) % 9)

val maskMessageMap = mapOf(
    "1$ZOS_USERID.*" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.{!" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.A23456789.*" to "Qualifier must be in 1 to 8 characters",
    "$ZOS_USERID." to ENTER_VALID_DS_MASK_MESSAGE,
    maskWithLength45 to "Dataset mask must be no more than 44 characters",
)

val validZOSMasks = listOf(
    "$ZOS_USERID.*", "$ZOS_USERID.**", "$ZOS_USERID.@#%", "$ZOS_USERID.@#%.*", "Q.*", "WWW.*", maskWithLength44,
    ZOS_USERID
)

val validUSSMasks = listOf("/u", "/uuu", "/etc/ssh", "/u/$ZOS_USERID")

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

fun deleteAllConnections(
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
                conTab.click()
                deleteAllItems()
            }
            clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
}

fun deleteAllWorkingSets(
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
            }
            clickButton("OK")
        }
        closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
    }
}

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