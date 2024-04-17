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
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ActionButtonFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.keyboard
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import workingset.EMPTY_DATASET_MESSAGE
import workingset.PROJECT_NAME
import java.awt.event.KeyEvent
import java.time.Duration

/**
 * Tests creating JES working sets and jobs filters from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class JesWorkingSetViaContextMenuTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Add JES Working Set Dialog", "Edit JES Working Set Dialog", "Edit Jobs Filter Dialog"
    )
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
    fun testAddJesWorkingSetWithoutConnectionViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
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
                createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
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
                }
                closableFixtureCollector.closeOnceIfExists(AddConnectionDialog.name)

                createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
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
     * Tests to add new empty JES working sets with different names, checks that correct message is returned.
     */
    @Test
    @Order(2)
    fun testAddEmptyJesWorkingSetsWithDifferentNamesViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createJWS("A".repeat(200), true, remoteRobot)
        createJWS("B12#$%^&*", true, remoteRobot)
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with one valid jobs filter.
     */
    @Test
    @Order(3)
    fun testAddJesWorkingSetWithOneValidFilterViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val filter = Triple("*", ZOS_USERID, "")
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, filter)
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
    }

    /**
     * Tests to add new JES working set with already existing name, checks that correct message is returned.
     */
    @Test
    @Order(4)
    fun testAddJWSWithTheSameNameViaContextMenu(remoteRobot: RemoteRobot) {
        createJWS("JWS1", false, remoteRobot)
    }

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    @Order(5)
    fun testAddJWSWithInvalidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
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
     * Tests to add new JES working set with several valid jobs filters, opens filters in explorer.
     */
    @Test
    @Order(6)
    fun testAddJWSWithValidFiltersViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs",
            { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: false },
            { MockResponse().setBody("[]") }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
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
     * Tests to add new JES working set with invalid connection, checks that correct message is returned.
     */
    @Test
    @Order(7)
    fun testAddJWSWithInvalidConnectionViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val jwsName = "JWS3"
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
                "invalid connection",
                false,
                remoteRobot,
                "https://${mockServer.hostName}:$testPort"
            )
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, "invalid connection", ZOS_USERID, Triple("*", ZOS_USERID, ""))
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

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to add new JES working set with the same jobs filters, checks that correct message is returned.
     */
    @Test
    @Order(8)
    fun testAddJWSWithTheSameFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS4"
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
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

    /**
     * Tests to create invalid jobs filters, checks that correct messages are returned.
     */
    @Test
    @Order(9)
    fun testCreateInvalidFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
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
    fun testCreateValidFiltersViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs",
            { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: false },
            { MockResponse().setBody("[]") }
        )
        validJobsFilters.forEach {
            createFilterFromContextMenu(jwsName, it, remoteRobot)
        }
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        validJobsFilters.forEach {
            openJobFilterInExplorer(it, "", fixtureStack, remoteRobot)
            closeFilterInExplorer(it, fixtureStack, remoteRobot)
        }
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to create already existing jobs filter in JES working set, checks that correct message is returned.
     */
    @Test
    @Order(11)
    fun testCreateAlreadyExistsFilterViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
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

    // TODO: eliminate ZOS_USERID
    /**
     * Tests to edit already existing JES working set by adding one jobs filter.
     */
    @Test
    @Order(12)
    fun testEditJWSAddOneFilterViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs",
            { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: false },
            { MockResponse().setBody("[]") }
        )
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        closeFilterInExplorer(Triple("*", ZOS_USERID, ""), fixtureStack, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editJWSFromContextMenu(jwsName, fixtureStack, closableFixtureCollector)
            editJesWorkingSetDialog(fixtureStack) {
                addFilter(ZOS_USERID, Triple("*", "$ZOS_USERID*", ""))
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
        }
        openJobFilterInExplorer(Triple("*", ZOS_USERID, ""), "", fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit already existing JES working set by deleting one jobs filter.
     */
    @Test
    @Order(13)
    fun testEditJWSDeleteJobsFilterViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        val jobsFilter =
            Triple("*", ZOS_USERID, "")
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editJWSFromContextMenu(jwsName, fixtureStack, closableFixtureCollector)
            editJesWorkingSetDialog(fixtureStack) {
                deleteFilter(jobsFilter)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
        }
        checkFilterWasDeletedJWSRefreshed(jobsFilter, fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit already existing JES working set by deleting all jobs filter.
     */
    @Test
    @Order(14)
    fun testEditJWSDeleteAllFiltersViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "first jws"
        val deletedFilters = listOf(
            Triple("TEST1", "$ZOS_USERID*", ""),
            Triple("", "", "JOB01234"),
            Triple("TEST*", ZOS_USERID, ""),
            Triple("TEST**", ZOS_USERID, ""),
            Triple("TEST***", ZOS_USERID, ""),
            Triple("TEST1", "$ZOS_USERID**", ""),
            Triple("TEST1", "$ZOS_USERID***", ""),
            Triple("TEST1", ZOS_USERID, ""),
            Triple("TEST***", "$ZOS_USERID***", "")
        )
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editJWSFromContextMenu(jwsName, fixtureStack, closableFixtureCollector)
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
        }
        deletedFilters.forEach { checkFilterWasDeletedJWSRefreshed(it, fixtureStack, remoteRobot) }
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit already existing JES working set by changing connection to invalid, checks that correct message returned.
     */
    @Test
    @Order(15)
    fun testEditJWSChangeConnectionToInvalidViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newConnectionName = "invalid connection"
        val jwsName = "JWS1"
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editJWSFromContextMenu(jwsName, fixtureStack, closableFixtureCollector)
            editJesWorkingSetDialog(fixtureStack) {
                changeConnection(newConnectionName)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
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
            fixtureStack,
            remoteRobot
        )
    }

    /**
     * Tests to edit already existing JES working set by changing connection to new one.
     */
    @Test
    @Order(16)
    fun testEditJWSChangeConnectionToNewValidViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val newConnectionName = "new $connectionName"
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
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restjobs",
                { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: false },
                { MockResponse().setBody("[]") }
            )
            createConnection(
                fixtureStack,
                closableFixtureCollector,
                newConnectionName,
                true,
                remoteRobot,
                "https://${mockServer.hostName}:${mockServer.port}"
            )
            val jwsName = "JWS1"
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
                editJWSFromContextMenu(jwsName, fixtureStack, closableFixtureCollector)
                editJesWorkingSetDialog(fixtureStack) {
                    changeConnection(newConnectionName)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
            }
            checkItemWasDeletedWSRefreshed("Invalid URL port: \"104431\"", fixtureStack, remoteRobot)
            openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        }

    /**
     * Tests to edit already existing JES working set by renaming it.
     */
    @Test
    @Order(17)
    fun testEditJWSRenameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val newJesWorkingSetName = "new jws name"
        val oldJesWorkingSetName = "JWS1"
        val alreadyExistsJesWorkingSetName = "JWS2"
        openOrCloseJesWorkingSetInExplorer(oldJesWorkingSetName, fixtureStack, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editJWSFromContextMenu(oldJesWorkingSetName, fixtureStack, closableFixtureCollector)
            editJesWorkingSetDialog(fixtureStack) {
                renameJesWorkingSet(alreadyExistsJesWorkingSetName)
                clickButton("OK")
                val message = find<HeavyWeightWindowFixture>(
                    byXpath("//div[@class='HeavyWeightWindow']"),
                    Duration.ofSeconds(30)
                ).findAllText()
                (message[0].text + message[1].text).shouldContain("You must provide unique working set name. Working Set $alreadyExistsJesWorkingSetName already exists.")
                renameJesWorkingSet(newJesWorkingSetName)
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(EditJesWorkingSetDialog.name)
        }
        checkItemWasDeletedWSRefreshed(oldJesWorkingSetName, fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(newJesWorkingSetName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to edit jobs filter in JES working set.
     */
    @Test
    @Order(18)
    fun testEditJobFilterViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "new jws name"
        val oldFilter = Triple("*", ZOS_USERID, "")
        val newFilter = Triple("**", "$ZOS_USERID*", "")
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs",
            { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: false },
            { MockResponse().setBody("[]") }
        )
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
        editJobFilter(oldFilter, newFilter, remoteRobot)
        checkFilterWasDeletedJWSRefreshed(oldFilter, fixtureStack, remoteRobot)
        openJobFilterInExplorer(newFilter, "", fixtureStack, remoteRobot)
        openOrCloseJesWorkingSetInExplorer(jwsName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete jobs filter from JES working set.
     */
    @Test
    @Order(19)
    fun testDeleteJobFilterFromJWSViaContextMenu(remoteRobot: RemoteRobot) {
        deleteJobFilterFromContextMenu("JWS2", Triple("*", ZOS_USERID, ""), remoteRobot)
    }

    /**
     * Tests to delete JES working set.
     */
    @Test
    @Order(20)
    fun testDeleteJWSViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS2"
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            deleteJWSFromContextMenu(jwsName)
            clickButton("Yes")
        }
        checkItemWasDeletedWSRefreshed(jwsName, fixtureStack, remoteRobot)
    }

    /**
     * Tests to delete all JES working sets.
     */
    @Test
    @Order(21)
    fun testDeleteAllJWSViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsList = listOf("first jws", "A".repeat(200), "B12#$%^&*", "new jws name", "JWS3")
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).click()
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                }
                find<ComponentFixture>(viewTree).rightClick()
            }
            actionMenuItem(remoteRobot, "Delete").click()
            jwsList.forEach {
                find<ComponentFixture>(byXpath("//div[@class='MyDialog' and @title='Deletion of JES Working Set $it']"))
                clickButton("Yes")
            }
            find<ComponentFixture>(viewTree).findText("Nothing to show")
        }
    }

    /**
     * Deletes job filter via context menu, checks that it's not displayed in explorer.
     */
    private fun deleteJobFilterFromContextMenu(
        jwsName: String,
        jobFilter: Triple<String, String, String>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val textToFind = convertJobFilterToString(jobFilter)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).findText(jwsName).doubleClick()
                Thread.sleep(3000)
                find<ComponentFixture>(viewTree).findText(textToFind).rightClick()
            }
            actionMenuItem(remoteRobot, "Delete").click()
            Thread.sleep(3000)
            dialog("Deletion Of Jobs Filter") {
                clickButton("Yes")
            }
            checkFilterWasDeletedJWSRefreshed(jobFilter, fixtureStack, remoteRobot)
            explorer {
                find<ComponentFixture>(viewTree).findText(jwsName).doubleClick()
            }
        }
    }

    private fun convertJobFilterToString(jobFilter: Triple<String, String, String>): String {
        val textToFind = if (jobFilter.third == "") {
            "PREFIX=${jobFilter.first} OWNER=${jobFilter.second}".uppercase()
        } else {
            "JobID=${jobFilter.third}"
        }
        return textToFind
    }

    /**
     * Edits job filter via context menu.
     */
    private fun editJobFilter(
        oldFilter: Triple<String, String, String>,
        newFilter: Triple<String, String, String>,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val textToFind = convertJobFilterToString(oldFilter)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).findText(textToFind).rightClick()
            }
            actionMenuItem(remoteRobot, "Edit").click()
            closableFixtureCollector.add(EditJobsFilterDialog.xPath(), fixtureStack)
            editJobsFilterDialog(fixtureStack) {
                createJobsFilter(newFilter)
                Thread.sleep(3000)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(EditJobsFilterDialog.name)
        }
    }

    /**
     * Creates empty JES working set from context menu.
     */
    private fun createJWS(jwsName: String, isUniqueName: Boolean, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName)
                clickButton("OK")
                if (isUniqueName) {
                    Thread.sleep(3000)
                    find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                        EMPTY_DATASET_MESSAGE
                    )
                    clickButton("OK")
                    Thread.sleep(3000)
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
        }
    }

    /**
     * Creates jobs filter in the JES working set from context menu.
     */
    private fun createFilterFromContextMenu(
        jwsName: String,
        filter: Triple<String, String, String>,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            ideFrameImpl(PROJECT_NAME, fixtureStack) {
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