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
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration


/**
 * Tests creating working sets and masks.
 */
@Tag("FirstTime")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class WorkingSetFromActionButtonTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add Working Set Dialog")

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
     * Closes the project and clear test environment.
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
    fun testAddWorkingSetWithoutConnectionFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "first ws"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addConnectionDialog(fixtureStack) {
                addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                clickButton("OK")
                Thread.sleep(5000)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).hasText("You are going to create a Working Set that doesn't fetch anything")
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }


    @Test
    @Order(2)
    fun testAddEmptyWorkingSetWithVeryLongNameFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName: String = "B".repeat(200)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).hasText("You are going to create a Working Set that doesn't fetch anything")
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    @Test
    @Order(3)
    fun testAddWorkingSetWithOneValidMaskFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS1"
        val mask = Pair("$ZOS_USERID.*", "z/OS")
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, mask)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    @Test
    @Order(4)
    fun testAddWorkingSetWithValidZOSMasksFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS2"
        val masks: ArrayList<Pair<String, String>> = ArrayList()
        //todo allocate dataset with 44 length

        validZOSMasks.forEach {
            masks.add(Pair(it, "z/OS"))
        }

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, masks)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
        //todo open masks in explorer
    }

    @Test
    @Order(5)
    fun testAddWorkingSetWithValidUSSMasksFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS3"
        val masks: ArrayList<Pair<String, String>> = ArrayList()

        validUSSMasks.forEach {
            masks.add(Pair(it, "USS"))
        }

        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, masks)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
        //todo open masks in explorer
    }


    @Test
    @Order(6)
    fun testAddWorkingSetWithInvalidMasksFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        //todo add mask *.* when bug is fixed
        val wsName = "WS4"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
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
        }
    }

    @Test
    @Order(7)
    fun testAddWorkingSetWithTheSameMasksFromActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val wsName = "WS4"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                createWorkingSet(closableFixtureCollector, fixtureStack)
            }
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
}
