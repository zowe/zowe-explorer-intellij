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
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests creating, editing and deleting working sets and masks from context menu.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class RenameDatasetTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf("Allocate DataSet Dialog", "Allocate Member Dialog")

    private val projectName = "untitled"
    private val connectionName = "con1"
    private val wsName = "WS name"
    private val errorHeader = "Error in plugin For Mainframe"
    private val errorType = "Unable to rename"

    private val pdsName = "$ZOS_USERID.UI.TEST".uppercase()
    private val memberName = "TESTM"
    private val memberFinalName = "TESTMF"
    private val anotherMemberName = "TESTMA"

    private val dsName = "$ZOS_USERID.UI.TESTD".uppercase()
    private val dsFinalName = "$ZOS_USERID.UI.TESTDF".uppercase()
    private val anotherDsName = "$ZOS_USERID.UI.TESTA".uppercase()
    private var mapListDatasets = mutableMapOf<String, String>()

    /**
     * Opens the project and Explorer, clears test environment.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        startMockServer()
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
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(
            projectName,
            fixtureStack,
            closableFixtureCollector,
            connectionName,
            true,
            remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
        )
        createWsWithoutMask(projectName, wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)

        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${pdsName}_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${pdsName}") ?: false },
            { MockResponse().setBody("{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
        )
        allocatePDSAndCreateMask(
            wsName,
            pdsName,
            projectName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot,
            "$ZOS_USERID.UI.TEST*",
            directory = 2,
            false
        )
        mapListDatasets[pdsName] = listDS(pdsName)
        allocateDSWithMock(testInfo, dsName, remoteRobot)
        allocateDSWithMock(testInfo, anotherDsName, remoteRobot)
        openWSAndListDatasets(testInfo, remoteRobot)
        allocateMemberWithMock(testInfo, pdsName, memberName, remoteRobot)
        allocateMemberWithMock(testInfo, pdsName, anotherMemberName, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        deleteDatasetsWithMock(testInfo, remoteRobot)
        mockServer.shutdown()
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
        responseDispatcher.removeAllEndpoints()
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests renaming member when valid member name is provided.
     */
    @Test
    @Order(1)
    fun testRenameMemberWithCorrectNameViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            var isFirstRequest = true
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_listmembers1",
                { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${pdsName}/member") ?: false && isFirstRequest },
                { MockResponse().setBody("{\"items\":[{\"member\": \"${memberName}\"},{\"member\": \"${anotherMemberName}\"}],\"returnedRows\": 2,\"JSONversion\": 1}") }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_listmembers2",
                { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${pdsName}/member") ?: false && !isFirstRequest },
                { MockResponse().setBody("{\"items\":[{\"member\": \"${memberFinalName}\"},{\"member\": \"${anotherMemberName}\"}],\"returnedRows\": 2,\"JSONversion\": 1}") }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_rename_member",
                { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${pdsName}(${memberFinalName})") ?: false },
                { MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${pdsName}\",\"member\":\"${memberName}\"}}") }
            )
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    fileExplorer.click()
                    Thread.sleep(3000)
                    find<ComponentFixture>(viewTree).findText(pdsName).doubleClick()
                    Thread.sleep(5000)
                    find<ComponentFixture>(viewTree).findText(memberName).rightClick()
                }
                actionMenuItem(remoteRobot, "Rename").click()
                dialog("Rename Member") {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = memberFinalName
                    isFirstRequest = false
                }
                clickButton("OK")
            }
        }

    /**
     * Tests renaming member to name of another member in the same PDS and validates error pop-up notification.
     */
    @Test
    @Order(2)
    fun testRenameMemberWithNameOfAnotherMemberViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val errorDetail = "Member already exists"
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_rename_member",
                { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${pdsName}(${anotherMemberName})") ?: false },
                {
                    MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${pdsName}\",\"member\":\"${memberFinalName}\"}}")
                        .setResponseCode(500)
                        .setBody("{\"category\":\"4.0\",\"message\":\"Rename member failed\",\"rc\":\"4.0\",\"details\":[\"ISRZ002 $errorDetail - Directory already contains the specified member name.\"],\"reason\":\"0.0\"}")
                }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_listmembers",
                { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${pdsName}/member") ?: false },
                { MockResponse().setBody("{\"items\":[{\"member\": \"${memberFinalName}\"},{\"member\": \"${anotherMemberName}\"}],\"returnedRows\": 2,\"JSONversion\": 1}") }
            )
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    Thread.sleep(5000)
                    find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
                }
                actionMenuItem(remoteRobot, "Rename").click()
                dialog("Rename Member") {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = anotherMemberName
                }
                clickButton("OK")
                Thread.sleep(3000)
                checkErrorNotification(errorHeader, errorType, errorDetail, projectName, fixtureStack, remoteRobot)
            }
        }

    /**
     * Tests renaming member to the same name and validates error pop-up notification.
     */
    @Test
    @Order(3)
    fun testRenameMemberWithTheSameNameViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val errorDetail = "Member in use"
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_rename_member",
                { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${pdsName}(${memberFinalName})") ?: false },
                {
                    MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${pdsName}\",\"member\":\"${memberFinalName}\"}}")
                        .setResponseCode(500)
                        .setBody("{\"category\":\"4.0\",\"message\":\"Rename member failed\",\"rc\":\"12.0\",\"details\":[\"ISRZ002 $errorDetail - Member is being updated by you or another user. Enter HELP for a list of users using the data set.\"],\"reason\":\"0.0\"}")
                }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_listmembers",
                { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${pdsName}/member") ?: false },
                { MockResponse().setBody("{\"items\":[{\"member\": \"${memberFinalName}\"},{\"member\": \"${anotherMemberName}\"}],\"returnedRows\": 2,\"JSONversion\": 1}") }
            )
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    Thread.sleep(5000)
                    find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
                }
                actionMenuItem(remoteRobot, "Rename").click()
                dialog("Rename Member") {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = memberFinalName
                }
                clickButton("OK")
                Thread.sleep(3000)
                checkErrorNotification(errorHeader, errorType, errorDetail, projectName, fixtureStack, remoteRobot)
            }
        }

    /**
     * Tests renaming member to very long and validates error notification.
     */
    @Test
    @Order(4)
    fun testRenameMemberWithTooLongNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "123456789"
            }
            clickButton("OK")
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                MEMBER_NAME_LENGTH_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming member to invalid name and validates error notification.
     */
    @Test
    @Order(5)
    fun testRenameMemberWithInvalidNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "@*"
            }
            clickButton("OK")
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                INVALID_MEMBER_NAME_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming member to name with the invalid first symbol and validates error notification.
     */
    @Test
    @Order(6)
    fun testRenameMemberWithInvalidFirstSymbolViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "**"
            }
            clickButton("OK")
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                INVALID_MEMBER_NAME_BEGINNING_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming member to empty name and validates error notification.
     */
    @Test
    @Order(7)
    fun testRenameMemberWithEmptyNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(memberFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Member") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = ""
            }
            clickButton("OK")
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                MEMBER_EMPTY_NAME_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming DataSet when valid member name is provided.
     */
    @Test
    @Order(8)
    fun testRenameDataSetWithCorrectNameViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_listmembers",
                { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${pdsName}/member") ?: false },
                { MockResponse().setBody("{\"items\":[{\"member\": \"${memberFinalName}\"},{\"member\": \"${anotherMemberName}\"}],\"returnedRows\": 2,\"JSONversion\": 1}") }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_rename_ds",
                { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${dsFinalName}") ?: false },
                { MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${dsName}\"}}") }
            )
            mapListDatasets.remove(dsName)
            mapListDatasets[dsFinalName] = listDS(dsFinalName)
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles",
                {
                    it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${"$ZOS_USERID.UI.TEST*".uppercase()}")
                        ?: false
                },
                { MockResponse().setBody(buildFinalListDatasetJson()) }
            )
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    Thread.sleep(3000)
                    find<ComponentFixture>(viewTree).findText(dsName).rightClick()
                }
                actionMenuItem(remoteRobot, "Rename").click()
                dialog("Rename Dataset") {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = dsFinalName
                }
                clickButton("OK")
            }
        }

    /**
     * Tests renaming DataSet to name of another DataSet and validates error pop-up notification.
     */
    @Test
    @Order(9)
    fun testRenameDatasetWithNameOfAnotherDatasetViaContextMenu(testInfo: TestInfo, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val errorDetail = "data set rename failed"
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_rename_ds",
                { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${anotherDsName}") ?: false },
                {
                    MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${dsFinalName}\"}}")
                        .setResponseCode(500)
                        .setBody("{\"category\":\"1.0\",\"message\":\"$errorDetail\",\"rc\":\"8.0\",\"details\":[\"EDC5051I An error occurred when renaming a file.\"],\"reason\":\"6.0\"}")
                }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_list_ds",
                {
                    it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${"$ZOS_USERID.UI.TEST*".uppercase()}")
                        ?: false
                },
                { MockResponse().setBody(buildFinalListDatasetJson()) }
            )
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    Thread.sleep(5000)
                    find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
                }
                actionMenuItem(remoteRobot, "Rename").click()
                dialog("Rename Dataset") {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = anotherDsName
                }
                clickButton("OK")
                Thread.sleep(3000)
                checkErrorNotification(errorHeader, errorType, errorDetail, projectName, fixtureStack, remoteRobot)
            }
        }

    /**
     * Tests renaming DataSet to name with invalid section and validates error notification.
     */
    @Test
    @Order(10)
    fun testRenameDatasetWithInvalidSectionViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "$dsFinalName.123456789"
            }
            clickButton("OK")
            var message = ""
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findAllText().forEach {
                message += it.text
            }
            Thread.sleep(3000)
            clickButton("Cancel")
            if (!message.contains(DATASET_INVALID_SECTION_MESSAGE)) {
                throw Exception("Error message is different from expected")
            }
        }
    }

    /**
     * Tests renaming DataSet to very long name and validates error notification.
     */
    @Test
    @Order(11)
    fun testRenameDatasetWithTooLongNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = "A".repeat(45)
            }
            clickButton("OK")
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                DATASET_NAME_LENGTH_MESSAGE
            )
            Thread.sleep(3000)
            clickButton("Cancel")
        }
    }

    /**
     * Tests renaming DataSet to empty name and validates error notification.
     */
    @Test
    @Order(12)
    fun testRenameDatasetWithEmptyNameViaContextMenu(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                Thread.sleep(5000)
                find<ComponentFixture>(viewTree).findText(dsFinalName).rightClick()
            }
            actionMenuItem(remoteRobot, "Rename").click()
            dialog("Rename Dataset") {
                find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = ""
            }
            clickButton("OK")
            Thread.sleep(3000)
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                MEMBER_EMPTY_NAME_MESSAGE
            )
            clickButton("Cancel")
        }
    }

    private fun allocateDSWithMock(testInfo: TestInfo, dsName: String, remoteRobot: RemoteRobot) {
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${dsName}_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${dsName}") ?: false },
            { MockResponse().setBody("{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
        )
        allocateDataSet(wsName, dsName, projectName, fixtureStack, remoteRobot)
        mapListDatasets[dsName] = listDS(dsName)
    }

    private fun openWSAndListDatasets(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles",
            {
                it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${"$ZOS_USERID.UI.TEST*".uppercase()}")
                    ?: false
            },
            { MockResponse().setBody(buildFinalListDatasetJson()) }
        )
        openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
    }

    private fun allocateMemberWithMock(
        testInfo: TestInfo,
        dsName: String,
        memberName: String,
        remoteRobot: RemoteRobot
    ) {
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_${memberName}in${dsName}_restfiles",
            { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${dsName}(${memberName})") ?: false },
            { MockResponse().setBody("{}") }
        )
        allocateMemberForPDS(pdsName, memberName, projectName, fixtureStack, remoteRobot)
    }

    private fun buildFinalListDatasetJson(): String {
        var result = "{}"
        if (mapListDatasets.isNotEmpty()) {
            var listDatasetsJson = "{\"items\":["
            mapListDatasets.forEach {
                listDatasetsJson += it.value
            }
            result = listDatasetsJson.dropLast(1) + "],\n" +
                    "  \"returnedRows\": ${mapListDatasets.size},\n" +
                    "  \"totalRows\": ${mapListDatasets.size},\n" +
                    "  \"JSONversion\": 1\n" +
                    "}"
        }
        return result
    }

    private fun deleteDatasetsWithMock(testInfo: TestInfo, remoteRobot: RemoteRobot) {
        val datasetsToBeDeleted = mutableListOf<String>()
        mapListDatasets.forEach { datasetsToBeDeleted.add(it.key) }
        responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restfiles_members",
            { it?.requestLine?.contains("/member") ?: false },
            { MockResponse().setBody("{\"items\":[],\"returnedRows\":0,\"totalRows\":0,\"moreRows\":null,\"JSONversion\":1}") }
        )
        datasetsToBeDeleted.forEach { entry ->
            mapListDatasets.remove(entry)
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_restfiles_${entry}",
                {
                    it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${"$ZOS_USERID.UI.TEST*".uppercase()}")
                        ?: false
                },
                { MockResponse().setBody(buildFinalListDatasetJson()) }
            )
            responseDispatcher.injectEndpoint(
                "${testInfo.displayName}_delete_${entry}",
                { it?.requestLine?.contains("DELETE /zosmf/restfiles/ds/${entry}") ?: false },
                { MockResponse().setBody("{}") }
            )
            deleteDataset(entry, projectName, fixtureStack, remoteRobot)
        }
    }
}
