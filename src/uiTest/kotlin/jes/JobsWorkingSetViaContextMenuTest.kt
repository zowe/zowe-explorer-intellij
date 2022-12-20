/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package jes

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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

/**
 * Tests creating jobs working sets and jobs filters from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class JobsWorkingSetViaContextMenuTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Add Jobs Working Set Dialog", "Edit Jobs Working Set Dialog"
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
     * Tests to add new jobs working set without connection, checks that correct message is returned.
     */
    @Test
    @Order(1)
    fun testAddJobsWorkingSetWithoutConnectionViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addConnectionDialog(fixtureStack) {
                addConnection(connectionName, CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
            addJobsWorkingSetDialog(fixtureStack) {
                addJobsWorkingSet(jwsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJobsWorkingSetDialog.name)
        }
    }

    /**
     * Tests to add new empty jobs working sets with different names, checks that correct message is returned.
     */
    @Test
    @Order(2)
    fun testAddEmptyJobsWorkingSetsWithDifferentNamesViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJWS("A".repeat(200), true, remoteRobot)
        createJWS("B12#$%^&*", true, remoteRobot)
    }

    /**
     * Tests to add new jobs working set with one valid jobs filter.
     */
    @Test
    @Order(3)
    fun testAddJobsWorkingSetWithOneValidFilterViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val filter = Triple("*", ZOS_USERID, "")
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJobsWorkingSetDialog(fixtureStack) {
                addJobsWorkingSet(jwsName, connectionName, filter)
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJobsWorkingSetDialog.name)
        }
    }

    /**
     * Tests to add new jobs working set with already existing name, checks that correct message is returned.
     */
    @Test
    @Order(4)
    fun testAddJWSWithTheSameNameViaContextMenu(remoteRobot: RemoteRobot) {
        createJWS("JWS1", false, remoteRobot)
    }

    /**
     * Tests to add new jobs working set with invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    @Order(5)
    fun testAddJWSWithInvalidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJobsWorkingSetDialog(fixtureStack) {
                addJobsWorkingSet(jwsName, connectionName)
                invalidJobsFiltersMap.forEach {
                    addFilter(it.key.first)
                    if (button("OK").isEnabled()) {
                        clickButton("OK")
                    } else {
                        findText("OK").moveMouse()
                    }
                    val textToMoveMouse = when (it.key.second) {
                        1 -> it.key.first.first
                        2 -> it.key.first.second
                        else -> it.key.first.third
                    }
                    findText(textToMoveMouse).moveMouse()
                    Thread.sleep(5000)
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow'][.//div[@class='Header']]")).findText(
                        it.value
                    )
                    assertFalse(button("OK").isEnabled())
                    findText("Prefix").click()
                    clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))

                }
                clickButton("Cancel")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJobsWorkingSetDialog.name)
        }
    }

    /**
     * Tests to add new jobs working set with several valid jobs filters, opens filters in explorer.
     */
    @Test
    @Order(6)
    fun testAddJWSWithValidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJobsWorkingSetDialog(fixtureStack) {
                addJobsWorkingSet(jwsName, connectionName, validJobsFilters)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJobsWorkingSetDialog.name)
        }
        openOrCloseWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        validJobsFilters.forEach {
            openJobFilterInExplorer(it, "", projectName, fixtureStack, remoteRobot)
            closeFilterInExplorer(it, projectName, fixtureStack, remoteRobot)
        }
        openOrCloseWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new jobs working set with invalid connection, checks that correct message is returned.
     */
    @Test
    @Order(7)
    fun testAddJWSWithInvalidConnectionViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS3"
        createConnection(projectName, fixtureStack, closableFixtureCollector, "invalid_connection", false, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJobsWorkingSetDialog(fixtureStack) {
                addJobsWorkingSet(jwsName, "invalid_connection", Triple("*", ZOS_USERID, ""))
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJobsWorkingSetDialog.name)
        }
        openOrCloseWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        findAll<ComponentFixture>(byXpath("//div[@class='MyComponent'][.//div[@accessiblename='Invalid URL port: \"104431\"' and @class='JEditorPane']]")).forEach {
            it.click()
            findAll<ActionButtonFixture>(
                byXpath("//div[@class='ActionButton' and @myicon= 'close.svg']")
            ).first().click()
        }
        openOrCloseWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new jobs working set with the same jobs filters, checks that correct message is returned.
     */
    @Test
    @Order(8)
    fun testAddJWSWithTheSameFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS4"
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJobsWorkingSetDialog(fixtureStack) {
                addJobsWorkingSet(jwsName, connectionName, Triple("*", ZOS_USERID.lowercase(), ""))
                addJobsWorkingSet(jwsName, connectionName, Triple("*", ZOS_USERID.lowercase(), ""))
                clickButton("OK")
                find<HeavyWeightWindowFixture>(
                    byXpath("//div[@class='HeavyWeightWindow']"),
                    Duration.ofSeconds(30)
                ).findText(IDENTICAL_FILTERS_MESSAGE)
                assertFalse(button("OK").isEnabled())
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(AddJobsWorkingSetDialog.name)
        }
    }

    /**
     * Tests to create invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    @Order(9)
    fun testCreateInvalidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        ideFrameImpl(projectName, fixtureStack) {
            createJobsFilter(jwsName, fixtureStack, closableFixtureCollector)
            createJobsFilterDialog(fixtureStack) {
                invalidJobsFiltersMap.forEach {
                    createJobsFilter(it.key.first)
                    Thread.sleep(3000)
                    if (button("OK").isEnabled()) {
                        clickButton("OK")
                    }
                    val textToMoveMouse = when (it.key.second) {
                        1 -> it.key.first.first
                        2 -> it.key.first.second
                        else -> it.key.first.third
                    }
                    findText(textToMoveMouse).moveMouse()
                    Thread.sleep(5000)
                    findAll<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).first().findText(
                        it.value
                    )
                    assertFalse(button("OK").isEnabled())
                }
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(CreateJobsFilterDialog.name)
        }
    }

    /**
     * Tests to create valid jobs filters from context menu.
     */
    @Test
    @Order(10)
    fun testCreateValidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        validJobsFilters.forEach {
            createFilterFromContextMenu(jwsName, it, remoteRobot)
        }
        openOrCloseWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        validJobsFilters.forEach {
            openJobFilterInExplorer(it, "", projectName, fixtureStack, remoteRobot)
            closeFilterInExplorer(it, projectName, fixtureStack, remoteRobot)
        }
        openOrCloseWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to create already exists jobs filter in jobs working set, checks that correct message is returned.
     */
    @Test
    @Order(11)
    fun testCreateAlreadyExistsFilterViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        ideFrameImpl(projectName, fixtureStack) {
            createJobsFilter(jwsName, fixtureStack, closableFixtureCollector)
            createJobsFilterDialog(fixtureStack) {
                createJobsFilter(Triple("*", ZOS_USERID, ""))
                clickButton("OK")
                assertFalse(button("OK").isEnabled())
                assertTrue(
                    find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findAllText().first().text == "Job Filter with provided data already exists."
                )
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(CreateJobsFilterDialog.name)
        }
    }

    //TODO add next tests when corresponding features are implemented
    /*
      fun testEditJWSAddOneFilterViaContextMenu(){}
      fun testEditJWSDeleteFiltersViaContextMenu(){}
      fun testEditJWSDeleteAllFiltersViaContextMenu(){}
      fun testEditJWSChangeConnectionToInvalidViaContextMenu(){}
      fun testEditJWSChangeConnectionToNewValidViaContextMenu(){}
      fun testEditJWSRenameViaContextMenu(){}
      fun testDeleteJWSViaContextMenu(){}
      fun testDeleteAllJWSViaContextMenu(){}*/

    /**
     * Creates empty jobs working set from context menu.
     */
    private fun createJWS(jwsName: String, isUniqueName: Boolean, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJobsWorkingSetDialog(fixtureStack) {
                addJobsWorkingSet(jwsName, connectionName)
                if (isUniqueName) {
                    clickButton("OK")
                    Thread.sleep(5000)
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                        EMPTY_DATASET_MESSAGE
                    )
                    clickButton("OK")
                    Thread.sleep(5000)
                } else {
                    val message = find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findAllText()
                    (message[0].text + message[1].text).shouldContain("You must provide unique working set name. Working Set $jwsName already exists.")
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
            }
            closableFixtureCollector.closeOnceIfExists(AddJobsWorkingSetDialog.name)
        }
    }

    /**
     * Creates jobs filter in the jobs working set from context menu.
     */
    private fun createFilterFromContextMenu(
        jwsName: String,
        filter: Triple<String, String, String>,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                createJobsFilter(jwsName, fixtureStack, closableFixtureCollector)
                createJobsFilterDialog(fixtureStack) {
                    createJobsFilter(filter)
                    Thread.sleep(3000)
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(CreateJobsFilterDialog.name)
            }
        }
}