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
 * Tests the ConnectionManager on UI level.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ConnectionManager {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private val wantToClose = listOf(
        "Settings Dialog", "Add Connection Dialog", "Error Creating Connection Dialog",
        "Edit Connection Dialog"
    )
    private val projectName = "untitled"

    /**
     * Opens the project and Explorer, clears test environment..
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
     * Test that should pass and leave a bunch of opened dialogs.
     */
    @Test
    @Order(1)
    fun testAddWrongConnection(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                    addConnection("a", "https://a.com", "a", "a", true)
                    clickButton("OK")
                }
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    findText("No such host is known (a.com)")
                    assertTrue(true)
                }
            }
        }
    }

    /**
     * Tests that checks whether it is possible on UI level to add two connections with the same name.
     */
    @Test
    @Order(2)
    fun testAddTwoConnectionsWithTheSameName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val connectionName = "a"
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
                    addConnection(connectionName, "https://a.com", "a", "a", true)
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    clickButton("Yes")
                }
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
                configurableEditor {
                    add(closableFixtureCollector, fixtureStack)
                }
                addConnectionDialog(fixtureStack) {
                    addConnection(connectionName, "https://b.com", "b", "b", true)
                    findText(connectionName).moveMouse()
                    find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findText("You must provide unique connection name. Connection $connectionName already exists.")
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to create connection with valid parameters.
     */
    @Test
    @Order(3)
    fun testAddValidConnection(remoteRobot: RemoteRobot) {
        createConnection(projectName, fixtureStack, closableFixtureCollector, "valid connection1", true, remoteRobot)
    }

    /**
     * Tests to create connection with spaces before and after connection url.
     */
    @Test
    @Order(4)
    fun testAddConnectionWithSpaces(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                    addConnection("valid connection2", "   $CONNECTION_URL   ", " $ZOS_USERID ", ZOS_PWD, true)
                    clickButton("OK")
                    Thread.sleep(10000)
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to create connection with invalid credentials, checks that correct message returned.
     */
    @Test
    @Order(5)
    fun testAddConnectionWithInvalidCredentials(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                    addConnection("invalid connection1", CONNECTION_URL, "a", "a", true)
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    findText("Credentials are not valid")
                    clickButton("Yes")
                }
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to create connection with unchecked checkBox for SSL certification, checks that correct message returned.
     */
    @Test
    @Order(6)
    fun testAddConnectionWithUncheckedSSL(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                    addConnection("invalid connection2", CONNECTION_URL, ZOS_USERID, ZOS_PWD, false)
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    (findAllText()[2].text + findAllText()[3].text).shouldContain("Unable to find valid certification path to requested target")
                    clickButton("Yes")
                }
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to create connection with very long name.
     */
    @Test
    @Order(7)
    fun testAddConnectionWithVeryLongName(remoteRobot: RemoteRobot) {
        createConnection(projectName, fixtureStack, closableFixtureCollector, "A".repeat(200), true, remoteRobot)
    }

    /**
     * Tests to create connection with invalid connection url, checks that correct message returned.
     */
    @Test
    @Order(8)
    fun testAddInvalidConnectionWithUrlByMask(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                    addConnection("invalid connection3", "zzz", "a", "a", true)
                    clickButton("OK")
                    Thread.sleep(2000)
                    val messages = find<HeavyWeightWindowFixture>(
                        byXpath("//div[@class='HeavyWeightWindow']"),
                        Duration.ofSeconds(30)
                    ).findAllText()
                    (messages[0].text + messages[1].text).shouldContain("Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443")
                    assertFalse(button("OK").isEnabled())
                    clickButton("Cancel")
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to edit valid connection to invalid, checks that correct message returned.
     */
    @Test
    @Order(9)
    fun testEditConnectionFromValidToInvalid(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    conTab.click()
                    editConnection("valid connection1", closableFixtureCollector, fixtureStack)
                }
                editConnectionDialog(fixtureStack) {
                    addConnection("invalid connection3", "${CONNECTION_URL}1", ZOS_USERID, ZOS_PWD, true)
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    findText("Invalid URL port: \"104431\"")
                    clickButton("Yes")
                }
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to edit invalid connection to valid.
     */
    @Test
    @Order(10)
    fun testEditConnectionFromInvalidToValid(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    conTab.click()
                    editConnection("invalid connection3", closableFixtureCollector, fixtureStack)
                }
                editConnectionDialog(fixtureStack) {
                    addConnection("valid connection1", CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                    clickButton("OK")
                    Thread.sleep(3000)
                }
                closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to edit connection by unchecking SSL certification checkbox and returning it to back,
     * check that correct message returned.
     */
    @Test
    @Order(11)
    fun testEditConnectionUncheckSSLandReturnBack(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    conTab.click()
                    editConnection("valid connection1", closableFixtureCollector, fixtureStack)
                }
                editConnectionDialog(fixtureStack) {
                    uncheckSSLBox()
                    clickButton("OK")
                }
                Thread.sleep(3000)
                closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
                errorCreatingConnectionDialog(closableFixtureCollector, fixtureStack) {
                    (findAllText()[2].text + findAllText()[3].text).shouldContain("Unable to find valid certification path to requested target")
                    clickButton("No")
                }
                Thread.sleep(3000)
                closableFixtureCollector.closeOnceIfExists(ErrorCreatingConnectionDialog.name)
                editConnectionDialog(fixtureStack) {
                    addConnection("valid connection1", CONNECTION_URL, ZOS_USERID, ZOS_PWD, true)
                    clickButton("OK")
                }
                Thread.sleep(3000)
                closableFixtureCollector.closeOnceIfExists(EditConnectionDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
    }

    /**
     * Tests to delete connection with unique connection url and without any working sets.
     */
    @Test
    @Order(12)
    fun testDeleteConnectionWithUniqueUrlWithoutWSandJWS(remoteRobot: RemoteRobot) {
        deleteConnection("invalid connection1", "", "", remoteRobot)
    }

    /**
     * Tests to delete connection with working set, check that correct message returned.
     */
    @Test
    @Order(13)
    fun testDeleteConnectionWithWorkingSet(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createWorkingSet("WS1", "valid connection1", remoteRobot)
        deleteConnection("valid connection1", "WS1", "", remoteRobot)
    }

    /**
     * Tests to delete connection with JES working set, check that correct message returned.
     */
    @Test
    @Order(14)
    fun testDeleteConnectionWithJesWorkingSet(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJesWorkingSet("JWS1", "valid connection2", remoteRobot)
        deleteConnection("valid connection2", "", "JWS1", remoteRobot)
    }

    /**
     * Tests to delete connection with working set and JES working set, check that correct message returned.
     */
    @Test
    @Order(15)
    fun testDeleteConnectionWithWSandJWS(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createConnection(projectName, fixtureStack, closableFixtureCollector, "valid connection", true, remoteRobot)
        createWorkingSet("WS2", "valid connection", remoteRobot)
        createJesWorkingSet("JWS2", "valid connection", remoteRobot)
        deleteConnection("valid connection", "WS2", "JWS2", remoteRobot)
    }

    /**
     * Tests to delete all connections.
     */
    @Test
    @Order(16)
    fun testDeleteAllConnections(remoteRobot: RemoteRobot) = with(remoteRobot) {
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

    /**
     * Deletes connection and check message if connection has any working sets.
     */
    private fun deleteConnection(connectionName: String, wsName: String, jwsName: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    settings(closableFixtureCollector, fixtureStack)
                }
                settingsDialog(fixtureStack) {
                    configurableEditor {
                        conTab.click()
                        deleteItem(connectionName)
                    }
                    if (wsName.isNotEmpty() && jwsName.isNotEmpty()) {
                        dialog("Warning") {
                            (findAllText()[2].text + findAllText()[4].text).shouldContain("The following Files working sets use selected connections:$wsName.")
                            (findAllText()[6].text + findAllText()[8].text).shouldContain("The following JES working sets use selected connections:$jwsName.")
                            clickButton("Yes")
                        }
                    } else if (wsName.isNotEmpty() && jwsName.isEmpty()) {
                        dialog("Warning") {
                            (findAllText()[2].text + findAllText()[4].text).shouldContain("The following Files working sets use selected connections:$wsName.")
                            clickButton("Yes")
                        }

                    } else if (wsName.isEmpty() && jwsName.isNotEmpty()) {
                        dialog("Warning") {
                            (findAllText()[2].text + findAllText()[4].text).shouldContain("The following JES working sets use selected connections:$jwsName.")
                            clickButton("Yes")
                        }
                    }
                    clickButton("OK")
                }
                closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
            }
        }

    /**
     * Creates JES working set.
     */
    private fun createJesWorkingSet(jwsName: String, connectionName: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
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
                        addJesWorkingSet(jwsName, connectionName, Triple("*", ZOS_USERID, ""))
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
     * Creates working set.
     */
    private fun createWorkingSet(wsName: String, connectionName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                    addWorkingSet(wsName, connectionName, Pair("$ZOS_USERID.*", "z/OS"))
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
