/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package workingset

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.stepsProcessing.step
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import testutils.ProcessManager
import workingset.auxiliary.components.dialogs.CreateDirectoryDialog
import workingset.auxiliary.components.dialogs.CreateFileDialog
import workingset.auxiliary.components.dialogs.CreateMaskSubDialog
import workingset.auxiliary.components.elements.ButtonElement
import workingset.testutils.injectAllocateUssFile
import workingset.testutils.injectErrorFileCreating
import workingset.testutils.injectListAllUssFiles
import java.util.stream.Stream

/**
 * Tests allocating uss file and directory with valid and invalid parameters.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class CreateUssFileAndDirTest :IdeaInteractionClass(){
    private val closableFixtureCollector = ClosableFixtureCollector()
    private val fixtureStack = mutableListOf<Locator>()
    private val mapListUssFiles = mutableMapOf<String, String>()
    override val wsName = "WS1"

    private val errorType = "Cannot allocate file"
    private val fileErrorDetail = "The specified file already exists"
    private val dirErrorDetail = "The specified directory already exists"

    companion object {

        private val fileNameA = "a".repeat(256)
        private val fileNameB = "b".repeat(256)
        private const val invalidFileName = "invalid/name"

        private val incorrectFileName = mapOf(
            Pair(FILE_NAME_LENGTH_MESSAGE, fileNameA),
            Pair(FILE_RESRVED_SYMBOL_MESSAGE, invalidFileName),
        )
        @JvmStatic
        fun valuesProviderIncorrectFileName(): Stream<Arguments> {
            return incorrectFileName.entries.stream().map { entry ->
                Arguments.of(entry.key, entry.value)
            }
        }

        private val incorrectDirName = mapOf(
            Pair(FILE_RESRVED_SYMBOL_MESSAGE, invalidFileName),
            Pair(    FILE_NAME_LENGTH_MESSAGE, fileNameB),
        )

        @JvmStatic
        fun valuesProviderIncorrectDirName(): Stream<Arguments> {
            return incorrectDirName.entries.stream().map { entry ->
                Arguments.of(entry.key, entry.value)
            }
        }
    }


    private val fileList = "{\"name\":\"$USS_FILE_NAME\",\"mode\":\"-rwxr--rw-\",\"size\":20,\"uid\":0,\"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
            "\"group\":\"OMVSGRP\",\"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirList = "{\"name\":\"$USS_DIR_NAME\",\"mode\":\"drwxr--rw-\",\"size\":888, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
            "\"group\":\"OMVSGRP\",\"mtime\":\"2013-05-07T11:23:08\"},"
    private val dirHereList = "{\"name\":\".\", \"mode\":\"drwxrwxrwx\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
            "\"group\":\"OMVSGRP\", \"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirParentList = "{\"name\":\"..\", \"mode\":\"drwxr-xr-x\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
            "\"group\":\"OMVSGRP\", \"mtime\":\"2015-09-15T02:38:29\"},"
    private val ussFileExistError = "{\"category\":\"1.0\",\"message\":\"The specified file already exists\",\"rc\":\"4.0\",\"reason\":\"19.0\"}"
    private val ussFileSpecSymbolError = "{\"category\":\"1.0\",\"message\":\"The specified directory already exists\",\"rc\":\"4.0\",\"reason\":\"19.0\"}"
    private lateinit var processManager: ProcessManager

    /**
     * Opens the project and Explorer, clears test environment, creates uss mask.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        responseDispatcher.injectTestInfo(testInfo)
        responseDispatcher.injectTestInfoRestTopology(testInfo)
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(
            fixtureStack,
            closableFixtureCollector,
            connectionName,
            true,
            remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
        )
        createMaskSubDialog = CreateMaskSubDialog(fixtureStack, remoteRobot)
        createFileDialog = CreateFileDialog(fixtureStack, remoteRobot)
        createDirectoryDialog = CreateDirectoryDialog(fixtureStack, remoteRobot)

        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        mapListUssFiles["."] = dirHereList
        mapListUssFiles[".."] = dirParentList
//        injectListAllUssFiles(mapListUssFiles)
        callCreateMask(wsName, fixtureStack, remoteRobot)
        createMaskSubDialog.setMask(singleUssMask)
        createMaskSubDialog.clickButtonByName(OK_TEXT)
        compressAndDecompressTree(wsName, fixtureStack, remoteRobot)

        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        processManager.close()
        mockServer.shutdown()
    }

    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) = with(remoteRobot){
        responseDispatcher.removeAllEndpoints()
        refreshWorkSpace(wsName,  fixtureStack, remoteRobot)
    }

    /**
     * Test to create uss file with valid parameters
     */
    @Test
    fun testCreateUssFile(remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectAllocateUssFile(ussMask, USS_FILE_NAME)
        injectListAllUssFiles(mapListUssFiles)
        createUssFileForMask(ussMask,USS_FILE_NAME, remoteRobot)

        mapListUssFiles[USS_FILE_NAME] = fileList
        refreshWorkSpace(wsName, fixtureStack, remoteRobot)
    }

    /**
     * Test to create uss file with too long name and create uss file with invalid name
     */
    @ParameterizedTest
    @MethodSource("valuesProviderIncorrectFileName")
    fun testCreateUssFileWithIncorrectFileName(errorText: String, invalidValue: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectListAllUssFiles(mapListUssFiles)
        createUssFileForMask(ussMask,invalidValue, remoteRobot)

        find<HeavyWeightWindowFixture>(messageLoc).findText(errorText)
        createFileDialog.clickButtonByName(CANCEL_TEXT)
    }

    /**
     * Test to create uss directory with invalid name and
     * with too long name
     */
    @ParameterizedTest
    @MethodSource("valuesProviderIncorrectDirName")
    fun testCreateUssDirWithIncorrectDirName(errorText: String, invalidValue: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectListAllUssFiles(mapListUssFiles)
        step("try create with long dir name"){
            createUssDirForMask(ussMask, invalidValue, remoteRobot)
            find<HeavyWeightWindowFixture>(messageLoc).findText(errorText)
            createDirectoryDialog.clickButtonByName(CANCEL_TEXT)
        }
    }

    /**
     * Test to create uss file when file with the same name already exists
     */
    @Test
    fun testCreateUssFileWithExistingFileName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        step("Injection for tests"){
            injectListAllUssFiles(mapListUssFiles)
            injectErrorFileCreating(ussMask, USS_FILE_NAME,500, ussFileExistError)
            injectAllocateUssFile(ussMask, USS_FILE_NAME)
        }
        step("try create file"){
            createUssFileForMask(ussMask, USS_FILE_NAME, remoteRobot)
            cancelButton.click()
        }
        assertTrue(isErrorNotificationValid(fileErrorDetail, errorType, fileErrorDetail, fixtureStack, remoteRobot))
        closeNotificztion(fixtureStack, remoteRobot)
    }

    /**
     * Test to create uss directory with valid parameters
     */
    @Test
    fun testCreateUssDir(remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectListAllUssFiles(mapListUssFiles)
        injectAllocateUssFile(ussMask, USS_DIR_NAME)
        createUssDirForMask(ussMask, USS_DIR_NAME, remoteRobot)

        mapListUssFiles[USS_DIR_NAME] = dirList

        refreshWorkSpace(wsName,fixtureStack, remoteRobot)
    }

    /**
     * Test to create uss file when directory with the same name already exists
     */
    @Test
    fun testCreateUssFileWithExistingDirName(remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectListAllUssFiles(mapListUssFiles)
        injectErrorFileCreating(ussMask, USS_DIR_NAME,500, ussFileExistError)

        step("try create file"){
            createUssDirForMask(ussMask, USS_DIR_NAME, remoteRobot)
            cancelButton.click()
        }
        assertTrue(isErrorNotificationValid(fileErrorDetail, errorType, fileErrorDetail, fixtureStack, remoteRobot))
        closeNotificztion(fixtureStack, remoteRobot)
    }

    /**
     * Test to create uss directory when file with the same name already exists and
     * when directory with the same name already exists
     */
    @ParameterizedTest
    @ValueSource(strings = [USS_FILE_NAME, USS_DIR_NAME])
    fun testCreateUssDirWithExistingFileName(itemName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        injectListAllUssFiles(mapListUssFiles)

        injectErrorFileCreating(ussMask, itemName,500, ussFileSpecSymbolError)
        step("try create directory"){
            createUssDirForMask(ussMask, itemName, remoteRobot)
            cancelButton.click()
        }
        assertTrue(isErrorNotificationValid(dirErrorDetail, errorType, dirErrorDetail, fixtureStack, remoteRobot))
        closeNotificztion(fixtureStack, remoteRobot)
    }
}
