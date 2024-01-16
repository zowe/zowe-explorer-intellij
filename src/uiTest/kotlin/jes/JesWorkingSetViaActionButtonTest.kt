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
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import workingset.EMPTY_DATASET_MESSAGE
import workingset.PROJECT_NAME
import java.time.Duration


/**
 * Tests creating JES working sets and filters via action button.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class JesWorkingSetViaActionButtonTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Add JES Working Set Dialog")

    private val projectName = "untitled"
    private val connectionName = "valid connection"

    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        startMockServer()
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        mockServer.shutdown()
        clearEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            close()
        }
    }

    /**
     * Closes all unclosed closable fixtures that we want to close.
     */
    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) {
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests to add new JES working set without connection, checks that correct message is returned.
     */
    @Test
    @Order(1)
    fun testAddJesWorkingSetWithoutConnectionViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val jwsName = "first jws"
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_info",
                { it?.requestLine?.contains("zosmf/info") ?: false },
                { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_resttopology",
                { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false },
                { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
            )
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)

                try {
                    if (dialog("Add JES Working Set Dialog").isShowing) {
                        assertTrue(false)
                    }
                } catch (e: WaitForConditionTimeoutException) {
                    e.message.shouldContain("Failed to find 'Dialog' by 'title Add JES Working Set Dialog'")
                } finally {
                    closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                }

                createConnectionFromActionButton(closableFixtureCollector, fixtureStack)
                addConnectionDialog(fixtureStack) {
                    addConnection(
                        connectionName,
                        "https://${mockServer.hostName}:${mockServer.port}",
                        ZOS_USERID,
                        ZOS_PWD,
                        true
                    )
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName)
                    clickButton("OK")
                    Thread.sleep(3000)
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                        EMPTY_DATASET_MESSAGE
                    )
                    clickButton("OK")
                    Thread.sleep(3000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
            }
        }

    /**
     * Tests to add new empty JES working set with very long name, checks that correct message is returned.
     */
    @Test
    @Order(2)
    fun testAddEmptyJesWorkingSetWithVeryLongNameViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName: String = "B".repeat(200)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with one valid filter.
     */
    @Test
    @Order(3)
    fun testAddJesWorkingSetWithOneValidFilterViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val filter = Triple("*", ZOS_USERID, "")
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, filter)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with several valid filters, opens filters in explorer.
     */
    @Test
    @Order(4)
    fun testAddJesWorkingSetWithValidFiltersViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val jwsName = "JWS2"
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restjobs",
                { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: false },
                { MockResponse().setBody("[]") }
            )
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName, ZOS_USERID, validJobsFilters)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
            }
            openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
            validJobsFilters.forEach {
                openJobFilterInExplorer(it, "", fixtureStack, remoteRobot)
                closeFilterInExplorer(it, fixtureStack, remoteRobot)
            }
            openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with invalid filters, checks that correct messages are returned.
     */
    @Test
    @Order(5)
    fun testAddJesWorkingSetWithInvalidFiltersViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS3"
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName)
                invalidJobsFiltersMap.forEach {
                    addFilter(ZOS_USERID, it.key.first)
                    if (button("OK").isEnabled()) {
                        clickButton("OK")
                    } else {
                        findText("OK").moveMouse()
                    }
                    val textToMoveMouse = when (it.key.second) {
                        1 -> it.key.first.first
                        2 -> it.key.first.second.uppercase()
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
        }
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add JES working set with the same filters, checks that correct message is returned.
     */
    @Test
    @Order(6)
    fun testAddJesWorkingSetWithTheSameFiltersViaActionButton(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS3"
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, Triple("*", ZOS_USERID.lowercase(), ""))
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, Triple("*", ZOS_USERID.lowercase(), ""))
                clickButton("OK")
                find<HeavyWeightWindowFixture>(
                    byXpath("//div[@class='HeavyWeightWindow']"),
                    Duration.ofSeconds(30)
                ).findText(IDENTICAL_FILTERS_MESSAGE)
                assertFalse(button("OK").isEnabled())
                clickButton("Cancel")
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add JES working set with invalid connection, checks that correct message is returned.
     */
    @Test
    @Order(7)
    fun testAddJWSWithInvalidConnectionViaActionButton(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val testPort = "10443"
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_info",
                { it?.requestLine?.contains("zosmf/info") ?: false },
                { MockResponse().setBody("Invalid URL port: \"${testPort}1\"") }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restjobs",
                { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: false },
                { MockResponse().setBody("[]") }
            )
            createConnection(
                fixtureStack,
                closableFixtureCollector,
                "invalid_connection",
                false,
                remoteRobot,
                "https://${mockServer.hostName}:$testPort"
            )
            val jwsName = "JWS3"
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, "invalid_connection", ZOS_USERID, Triple("*", ZOS_USERID, ""))
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
            }
            openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
            findAll<ComponentFixture>(byXpath("//div[@class='MyComponent'][.//div[@accessiblename='Invalid URL port: \"104431\"' and @class='JEditorPane']]")).forEach {
                it.click()
                findAll<ActionButtonFixture>(
                    byXpath("//div[@class='ActionButton' and @myicon= 'close.svg']")
                ).first().click()
            }
            openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        }
}
