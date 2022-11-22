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
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

/**
 * Tests creating JES working sets and jobs filters via settings.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class JesWorkingSetViaSettingsTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Settings Dialog", "Add JES Working Set Dialog", "Edit JES Working Set Dialog"
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
     * Tests to add new JES working set without connection, checks that correct message is returned.
     */
    @Test
    @Order(1)
    fun testAddJesWorkingSetWithoutConnectionViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet("JWS1", "")
                    clickButton("OK")
                    comboBox("Specify connection").click()
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText("You must provide a connection")
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to add new empty JES working sets with different names, checks that correct message is returned.
     */
    @Test
    @Order(2)
    fun testAddEmptyJesWorkingSetsWithDifferentNamesViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createConnection(projectName, fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        createJWS("A".repeat(200), true, remoteRobot)
        createJWS("B12#$%^&*", true, remoteRobot)
    }

    /**
     * Tests to add new JES working set with one valid jobs filter.
     */
    @Test
    @Order(3)
    fun testAddJesWorkingSetWithOneValidFilterViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val filter = Triple("*", ZOS_USERID, "")
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName, filter)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to add new JES working set with already existing name, checks that correct message is returned.
     */
    @Test
    @Order(4)
    fun testAddJWSWithTheSameNameViaSettings(remoteRobot: RemoteRobot) {
        createJWS("JWS1", false, remoteRobot)
    }

    /**
     * Tests to add new JES working set with invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    @Order(5)
    fun testAddJWSWithInvalidFiltersViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                jesExplorer.click()
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName)
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
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to add new JES working set with several valid jobs filters, opens filters in explorer.
     */
    @Test
    @Order(6)
    fun testAddJWSWithValidFiltersViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                jesExplorer.click()
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName, validJobsFilters)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        validJobsFilters.forEach {
            openJobFilterInExplorer(it, "", projectName, fixtureStack, remoteRobot)
            closeFilterInExplorer(it, projectName, fixtureStack, remoteRobot)
        }
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new JES working set with invalid connection, checks that correct message is returned.
     */
    @Test
    @Order(7)
    fun testAddJWSWithInvalidConnectionViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createConnection(projectName, fixtureStack, closableFixtureCollector, "invalid_connection", false, remoteRobot)
        val jwsName = "JWS3"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, "invalid_connection", Triple("*", ZOS_USERID, ""))
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        findAll<ComponentFixture>(byXpath("//div[@class='MyComponent'][.//div[@accessiblename='Invalid URL port: \"104431\"' and @class='JEditorPane']]")).forEach {
            it.click()
            findAll<ActionButtonFixture>(
                byXpath("//div[@class='ActionButton' and @myicon= 'close.svg']")
            ).first().click()
        }
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to add new JES working set with the same jobs filters, checks that correct message is returned.
     */
    @Test
    @Order(8)
    fun testAddJWSWithTheSameFiltersViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS4"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName, Triple("*", ZOS_USERID.lowercase(), ""))
                    addJesWorkingSet(jwsName, connectionName, Triple("*", ZOS_USERID.lowercase(), ""))
                    clickButton("OK")
                    find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findText(IDENTICAL_FILTERS_MESSAGE)
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to edit JES working set by adding one job filter, checks that jws is refreshed, opens new filter.
     */
    @Test
    @Order(9)
    fun testEditJWSAddOneFilterViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val newFilter = Triple("TEST1", ZOS_USERID, "")
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        closeFilterInExplorer(Triple("*", ZOS_USERID, ""), projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    editWorkingSet(jwsName, closableFixtureCollector, fixtureStack)
                }
                editJesWorkingSetDialog(fixtureStack) {
                    addFilter(newFilter)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        openJobFilterInExplorer(newFilter, "", projectName, fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit JES working set by deleting several filters, checks that jws is refreshed and filters were deleted.
     */
    @Test
    @Order(10)
    fun testEditJWSDeleteFiltersViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        val filtersToBeDeleted =
            listOf(Triple("*", ZOS_USERID, ""), Triple("TEST**", ZOS_USERID, ""), Triple("TEST***", ZOS_USERID, ""))
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    editJesWorkingSet(jwsName, closableFixtureCollector, fixtureStack)
                }
                editJesWorkingSetDialog(fixtureStack) {
                    deleteFilters(filtersToBeDeleted)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        filtersToBeDeleted.forEach { checkFilterWasDeletedJWSRefreshed(it, projectName, fixtureStack, remoteRobot) }
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit JES working set by deleting all filters, checks that jws is refreshed and filters were deleted.
     */
    @Test
    @Order(11)
    fun testEditJWSDeleteAllFiltersViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        val filtersToBeDeleted = listOf(
            Triple("TEST1", ZOS_USERID, ""),
            Triple("", "", "JOB01234"),
            Triple("TEST*", ZOS_USERID, ""),
            Triple("TEST1", "$ZOS_USERID*", ""),
            Triple("TEST1", "$ZOS_USERID**", ""),
            Triple("TEST1", "$ZOS_USERID***", ""),
            Triple("TEST***", "$ZOS_USERID***", "")
        )
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    editJesWorkingSet(jwsName, closableFixtureCollector, fixtureStack)
                }
                editJesWorkingSetDialog(fixtureStack) {
                    deleteAllFilters()
                    clickButton("OK")
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                        EMPTY_DATASET_MESSAGE
                    )
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        filtersToBeDeleted.forEach { checkFilterWasDeletedJWSRefreshed(it, projectName, fixtureStack, remoteRobot) }
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit JES working set by changing connection to invalid, checks that correct message is returned.
     */
    @Test
    @Order(12)
    fun testEditJWSChangeConnectionToInvalidViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    editJesWorkingSet(jwsName, closableFixtureCollector, fixtureStack)
                }
                editJesWorkingSetDialog(fixtureStack) {
                    changeConnection("invalid_connection")
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
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
        openJobFilterInExplorer(
            Triple("*", ZOS_USERID, ""),
            "Invalid URL port: \"104431\"",
            projectName,
            fixtureStack,
            remoteRobot
        )
    }

    /**
     * Tests to edit JES working set by changing connection from invalid to valid, checks that jws is refreshed in explorer and error message disappeared.
     */
    @Test
    @Order(13)
    fun testEditJWSChangeConnectionToNewValidViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val newConnectionName = "new $connectionName"
        createConnection(projectName, fixtureStack, closableFixtureCollector, newConnectionName, true, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    editJesWorkingSet(jwsName, closableFixtureCollector, fixtureStack)
                }
                editJesWorkingSetDialog(fixtureStack) {
                    changeConnection(newConnectionName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        checkItemWasDeletedWSRefreshed("Invalid URL port: \"104431\"", projectName, fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit JES working set by renaming it, checks that jws is refreshed in explorer.
     */
    @Test
    @Order(14)
    fun testEditJWSRenameViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newJesWorkingSetName = "new jws name"
        val oldJesWorkingSetName = "JWS1"
        val alreadyExistsJesWorkingSetName = "JWS2"
        openOrCloseJesWorkingSetInExplorer(oldJesWorkingSetName, projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    editJesWorkingSet(oldJesWorkingSetName, closableFixtureCollector, fixtureStack)
                }
                editJesWorkingSetDialog(fixtureStack) {
                    renameJesWorkingSet(alreadyExistsJesWorkingSetName)
                    val message = find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findAllText()
                    (message[0].text + message[1].text).shouldContain("You must provide unique working set name. Working Set $alreadyExistsJesWorkingSetName already exists.")
                    renameJesWorkingSet(newJesWorkingSetName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        checkItemWasDeletedWSRefreshed(oldJesWorkingSetName, projectName, fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(newJesWorkingSetName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete JES working set, checks that explorer info is refreshed.
     */
    @Test
    @Order(15)
    fun testDeleteJWSViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    deleteItem(jwsName)
                }
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        checkItemWasDeletedWSRefreshed(jwsName, projectName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete all JES working sets, checks that explorer info is refreshed.
     */
    @Test
    @Order(16)
    fun testDeleteAllJWSViaSettings(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    deleteAllItems()
                }
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
            find<ComponentFixture>(viewTree).findText("Nothing to show")
        }
    }

    /**
     * Creates empty JES working set via settings.
     */
    private fun createJWS(jwsName: String, isUniqueName: Boolean, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName)
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
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }
}