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
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests allocating uss file and directory with valid and invalid parameters.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class CreateUssFileAndDirTest {
    private val closableFixtureCollector = ClosableFixtureCollector()
    private val fixtureStack = mutableListOf<Locator>()
    private val mapListUssFiles = mutableMapOf<String, String>()

    private val projectName = "untitled"
    private val connectionName = "con1"
    private val wsName = "WS name"
    private val ussMaskName = "/u/${ZOS_USERID.uppercase()}"
    private val ussFileName = "testFile"
    private val ussDirName = "testFolder"
    private val invalidFileName = "invalid/name"
    private val errorHeader = "Error in plugin For Mainframe"
    private val errorType = "Cannot allocate file"
    private val fileErrorDetail = "The specified file already exists"
    private val dirErrorDetail = "The specified directory already exists"


    private val fileList = "{\"name\":\"$ussFileName\",\"mode\":\"-rwxr--rw-\",\"size\":20,\"uid\":0,\"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
            "\"group\":\"OMVSGRP\",\"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirList = "{\"name\":\"$ussDirName\",\"mode\":\"drwxr--rw-\",\"size\":888, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
            "\"group\":\"OMVSGRP\",\"mtime\":\"2013-05-07T11:23:08\"},"
    private val dirHereList = "{\"name\":\".\", \"mode\":\"drwxrwxrwx\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
            "\"group\":\"OMVSGRP\", \"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirParentList = "{\"name\":\"..\", \"mode\":\"drwxr-xr-x\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
            "\"group\":\"OMVSGRP\", \"mtime\":\"2015-09-15T02:38:29\"},"

    /**
     * Opens the project and Explorer, clears test environment, creates uss mask.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
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
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(
            fixtureStack,
            closableFixtureCollector,
            connectionName,
            true,
            remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
        )
        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        mapListUssFiles["."] = dirHereList
        mapListUssFiles[".."] = dirParentList
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair(ussMaskName, "USS"))
                clickButton("OK")
                Thread.sleep(1000)
            }
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).doubleClick()
                Thread.sleep(1000)
            }
        }
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

    @AfterEach
    fun tearDown() {
        responseDispatcher.removeAllEndpoints()
    }

    /**
     * Test to create uss file with valid parameters
     */
    @Test
    @Order(1)
    fun testCreateUssFile(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "allocateUssFile_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs$ussMaskName/$ussFileName") ?: false },
            { MockResponse().setResponseCode(201) }
        )
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createUssFile(ussMaskName, ussFileName, UssFileType.File, fixtureStack, remoteRobot)
            mapListUssFiles[ussFileName] = fileList
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenuItem(remoteRobot, "Refresh").click()
        }
    }

    /**
     * Test to create uss file with too long name
     */
    @Test
    @Order(2)
    fun testCreateUssFileWithLongName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.File.name).click()
            createUssFileDialog(fixtureStack) {
                createFile("a".repeat(256), "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                FILE_NAME_LENGTH_MESSAGE
            )
            Thread.sleep(1000)
            clickButton("Cancel")
        }
    }

    /**
     * Test to create uss file with invalid name
     */
    @Test
    @Order(3)
    fun testCreateUssFileWithReservedSymbol(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.File.name).click()
            createUssFileDialog(fixtureStack) {
                createFile(invalidFileName, "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                FILE_RESRVED_SYMBOL_MESSAGE
            )
            Thread.sleep(1000)
            clickButton("Cancel")
        }
    }

    /**
     * Test to create uss file when file with the same name already exists
     */
    @Test
    @Order(4)
    fun testCreateUssFileWithExistingFileName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        responseDispatcher.injectEndpoint(
            "CreateFileWhenDirWithTheSameNameExists_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs${ussMaskName}/${ussFileName}") ?: false },
            {
                MockResponse().setResponseCode(500)
                    .setBody("{\"category\":\"1.0\",\"message\":\"The specified file already exists\",\"rc\":\"4.0\",\"reason\":\"19.0\"}")
            }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.File.name).click()
            createUssFileDialog(fixtureStack) {
                createFile(ussFileName, "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            clickButton("Cancel")
            checkErrorNotification(fileErrorDetail, errorType, fileErrorDetail, fixtureStack, remoteRobot)
            closeNotificztion(fixtureStack, remoteRobot)
        }
    }

    /**
     * Test to create uss directory with valid parameters
     */
    @Test
    @Order(5)
    fun testCreateUssDir(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        responseDispatcher.injectEndpoint(
            "allocateUssDir_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs$ussMaskName/$ussDirName") ?: false },
            { MockResponse().setResponseCode(201) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createUssFile(ussMaskName, ussDirName, UssFileType.Directory, fixtureStack, remoteRobot)
            mapListUssFiles[ussDirName] = dirList
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(wsName).rightClick()
            }
            actionMenuItem(remoteRobot, "Refresh").click()
        }
    }

    /**
     * Test to create uss file when directory with the same name already exists
     */
    @Test
    @Order(6)
    fun testCreateUssFileWithExistingDirName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        responseDispatcher.injectEndpoint(
            "FileAlreadyExists_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs${ussMaskName}/${ussDirName}") ?: false },
            {
                MockResponse().setResponseCode(500)
                    .setBody("{\"category\":\"1.0\",\"message\":\"The specified file already exists\",\"rc\":\"4.0\",\"reason\":\"19.0\"}")
            }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.File.name).click()
            createUssFileDialog(fixtureStack) {
                createFile(ussDirName, "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            clickButton("Cancel")
            checkErrorNotification(fileErrorDetail, errorType, fileErrorDetail, fixtureStack, remoteRobot)
            closeNotificztion(fixtureStack, remoteRobot)
        }
    }

    /**
     * Test to create uss directory with too long name
     */
    @Test
    @Order(7)
    fun testCreateUssDirWithLongName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.Directory.name).click()
            createUssFileDialog(fixtureStack) {
                createFile("a".repeat(256), "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                FILE_NAME_LENGTH_MESSAGE
            )
            Thread.sleep(1000)
            clickButton("Cancel")
        }
    }

    /**
     * Test to create uss directory with invalid name
     */
    @Test
    @Order(8)
    fun testCreateUssDirWithReservedSymbol(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.Directory.name).click()
            createUssFileDialog(fixtureStack) {
                createFile(invalidFileName, "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                FILE_RESRVED_SYMBOL_MESSAGE
            )
            Thread.sleep(1000)
            clickButton("Cancel")
        }
    }

    /**
     * Test to create uss directory when file with the same name already exists
     */
    @Test
    @Order(9)
    fun testCreateUssDirWithExistingFileName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        responseDispatcher.injectEndpoint(
            "DirAlreadyExists_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs${ussMaskName}/${ussFileName}") ?: false },
            {
                MockResponse().setResponseCode(500)
                    .setBody("{\"category\":\"1.0\",\"message\":\"The specified directory already exists\",\"rc\":\"4.0\",\"reason\":\"19.0\"}")
            }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.Directory.name).click()
            createUssFileDialog(fixtureStack) {
                createFile(ussFileName, "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            clickButton("Cancel")
            checkErrorNotification(dirErrorDetail, errorType, dirErrorDetail, fixtureStack, remoteRobot)
            closeNotificztion(fixtureStack, remoteRobot)
        }
    }

    /**
     * Test to create uss directory when directory with the same name already exists
     */
    @Test
    @Order(10)
    fun testCreateUssDirWithExistingDirName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        responseDispatcher.injectEndpoint(
            "DirAlreadyExists_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs${ussMaskName}/${ussDirName}") ?: false },
            {
                MockResponse().setResponseCode(500)
                    .setBody("{\"category\":\"1.0\",\"message\":\"The specified directory already exists\",\"rc\":\"4.0\",\"reason\":\"19.0\"}")
            }
        )
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(ussMaskName).rightClick()
            }
            actionMenu(remoteRobot, "New").click()
            actionMenuItem(remoteRobot, UssFileType.Directory.name).click()
            createUssFileDialog(fixtureStack) {
                createFile(ussDirName, "READ_WRITE_EXECUTE", "READ", "READ_WRITE")
                clickButton("OK")
            }
            clickButton("Cancel")
            checkErrorNotification(dirErrorDetail, errorType, dirErrorDetail, fixtureStack, remoteRobot)
            closeNotificztion(fixtureStack, remoteRobot)
        }
    }
}