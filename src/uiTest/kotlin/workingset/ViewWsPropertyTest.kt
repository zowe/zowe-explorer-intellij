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
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.testutils.injectAllocateUssFile
import workingset.testutils.injectListAllAllocatedDatasets
import workingset.testutils.injectListAllUssFiles
import workingset.testutils.injectSingleSpecificMember

/**
 * Tests viewing dataset and uss file properties.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ViewWsPropertyTest : IdeaInteractionClass() {
    private val closableFixtureCollector = ClosableFixtureCollector()
    private val fixtureStack = mutableListOf<Locator>()
    private val mapListDatasets = mutableMapOf<String, String>()
    private val mapListDatasetMembers = mutableMapOf<String, String>()
    private val mapListUssFiles = mutableMapOf<String, String>()

    override val connectionName = "con1"
    override val wsName = "WS name"
    private val pdsName = "${ZOS_USERID.uppercase()}.UI.TEST"
    private val memberName = "TESTM"
    private val dsName = "${ZOS_USERID.uppercase()}.UI.TESTD"
    private val maskName = "${ZOS_USERID.uppercase()}.UI.TEST*"
    override val datasetName = "${ZOS_USERID.uppercase()}.UI.TEST"
    private val ussMaskName = "/u/${ZOS_USERID.uppercase()}"
    private val ussFileName = "testFile"
    private val ussDirName = "testFolder"

    private val memList =
        "{\"member\":\"$memberName\",\"vers\":1,\"mod\":0,\"c4date\":\"2015/08/12\",\"m4date\":\"2015/08/12\",\"cnorc\":22,\n" +
                "\"inorc\":22,\"mnorc\":0,\"mtime\":\"05:48\",\"msec\":\"43\",\"user\":\"IBMUSER\",\"sclm\":\"N\"},"
    private val fileList =
        "{\"name\":\"$ussFileName\",\"mode\":\"-rwxr--rw-\",\"size\":20,\"uid\":0,\"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
                "\"group\":\"OMVSGRP\",\"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirList =
        "{\"name\":\"$ussDirName\",\"mode\":\"drwxr--rw-\",\"size\":888, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
                "\"group\":\"OMVSGRP\",\"mtime\":\"2013-05-07T11:23:08\"},"
    private val dirHereList =
        "{\"name\":\".\", \"mode\":\"drwxrwxrwx\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
                "\"group\":\"OMVSGRP\", \"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirParentList =
        "{\"name\":\"..\", \"mode\":\"drwxr-xr-x\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
                "\"group\":\"OMVSGRP\", \"mtime\":\"2015-09-15T02:38:29\"},"
    private lateinit var processManager: ProcessManager


    /**
     * Opens the project and Explorer, clears test environment, creates working set with dataset and uss masks,
     * allocates dataset and pds with member, creates uss file and directory.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) {
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
        createWsWithoutMask(wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        injectListAllAllocatedDatasets(datasetName, mapListDatasets)

        responseDispatcher.injectEndpoint(
            "listAllDatasetMembers_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/$pdsName/member") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListDatasetMembers, false)) }
        )
        mapListUssFiles["."] = dirHereList
        mapListUssFiles[".."] = dirParentList
        injectListAllUssFiles(mapListUssFiles)
        injectSingleSpecificMember(pdsName, memberName)
        responseDispatcher.injectAllocationResultPds(pdsName)
        responseDispatcher.injectAllocationResultPo(
            SEQUENTIAL_ORG_SHORT,
            TRACKS_ALLOCATION_UNIT_SHORT,
            dsName,
            VB_RECORD_FORMAT_SHORT,
            255
        )
        injectAllocateUssFile(ussMaskName, ussFileName)
        injectAllocateUssFile(ussMaskName, ussDirName)
        mapListDatasets[pdsName] = listDS(pdsName, PDS_TYPE, PO_ORG_SHORT)
        allocatePDSAndCreateMask(
            wsName,
            pdsName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot,
            maskName,
            directory = 2
        )
        createMask(wsName, ussMaskName, fixtureStack, closableFixtureCollector, USS_MASK, remoteRobot)
        mapListDatasets[dsName] = listDS(dsName, "", SEQUENTIAL_ORG_SHORT)
        allocateDataSet(wsName, dsName, fixtureStack, remoteRobot)
        mapListDatasetMembers[memberName] = memList
        allocateMemberForPDS(pdsName, memberName, fixtureStack, remoteRobot)
        mapListUssFiles[ussFileName] = fileList
        mapListUssFiles[ussDirName] = dirList
        openTreesElement(pdsName, remoteRobot)
        openTreesElement(ussMaskName, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll() {
        processManager.close()
        mockServer.shutdown()
    }

    /**
     * Test to check if member properties in opened dialog and expected values are matching
     */
    @Test
    fun testViewMemberProperties(remoteRobot: RemoteRobot) {
        callTreesElementProperty(memberName, fixtureStack, remoteRobot)
        val memberPropertiesValid = isMemberPropertyValid(
            memberName,
            "1.0",
            "2015/08/12",
            "2015/08/12",
            "05:48",
            "IBMUSER",
            "22",
            "22",
            "0",
            remoteRobot
        )
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        Assertions.assertTrue(memberPropertiesValid)
    }

    /**
     * Test to check if dataset properties in opened dialog and expected values are matching
     */
    @Test
    fun testViewDatasetProperties(remoteRobot: RemoteRobot) {
        openPropertyDatasetName(dsName, fixtureStack, remoteRobot)
        val datasetPropertyValid = isDatasetPropertyValid(
            dsName, "<Unknown>", "TEST.CATALOG.MASTER", "TESTVOL",
            "3390", "Sequential (PS)", "VB", "255", "3200", "10",
            "TRACKS", "1", "1", "2021/11/15", "2021/11/17", "***None***", remoteRobot
        )
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        Assertions.assertTrue(datasetPropertyValid)
    }

    /**
     * Test to check if pds properties in opened dialog and expected values are matching
     */
    @Test
    fun testViewPdsProperties(remoteRobot: RemoteRobot) {
        openPropertyDatasetName(pdsName, fixtureStack, remoteRobot)
        val datasetPropertyValid = isDatasetPropertyValid(
            pdsName, "PDS", "TEST.CATALOG.MASTER", "TESTVOL",
            "3390", PO_ORG_FULL, "VB", "255", "3200", "10",
            "TRACKS", "1", "1", "2021/11/15", "2021/11/17", "***None***", remoteRobot
        )
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        Assertions.assertTrue(datasetPropertyValid)
    }

    /**
     * Test to check if uss file properties in opened dialog and expected values are matching
     */
    @Test
    fun testViewUssFileProperties(remoteRobot: RemoteRobot) {
        openPropertyDatasetName(ussFileName, fixtureStack, remoteRobot)
        val validPropertyStatus = isUssFilePropertyValid(
            ussFileName, ussMaskName.drop(1), "$ussMaskName/$ussFileName", "20 bytes",
            "2015-11-24T02:12:04", ZOS_USERID.uppercase(), "OMVSGRP", "1",
            "READ_WRITE_EXECUTE", "READ", "READ_WRITE", remoteRobot
        )
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        Assertions.assertTrue(validPropertyStatus)
    }

    /**
     * Test to check if uss directory properties in opened dialog and expected values are matching
     */
    @Test
    fun testViewUssDirProperties(remoteRobot: RemoteRobot) {
        openPropertyDatasetName(ussDirName, fixtureStack, remoteRobot)
        val validPropertyStatus = isUssFilePropertyValid(
            ussDirName, ussMaskName.drop(1),
            "$ussMaskName/$ussDirName", "888 bytes", "2013-05-07T11:23:08", ZOS_USERID.uppercase(),
            "OMVSGRP", "1", "READ_WRITE_EXECUTE", "READ", "READ_WRITE",
            remoteRobot
        )
        clickByText(OK_TEXT, fixtureStack, remoteRobot)
        Assertions.assertTrue(validPropertyStatus)
    }

    private fun isDatasetPropertyValid(
        dsName: String, dsNameType: String, catalogName: String, volumeSerials: String, deviceType: String,
        organization: String, recordFormat: String, recordLength: String, blockSize: String, sizeInTracks: String,
        spaceUnits: String, usedTracks: String, usedExtents: String, createDate: String, modDate: String,
        expirationDate: String, remoteRobot: RemoteRobot,
    ): Boolean = with(remoteRobot) {
        var isValid = false
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            datasetPropertiesDialog(fixtureStack) {
                isValid = areDatasetPropertiesValid(
                    dsName, dsNameType, catalogName, volumeSerials, deviceType, organization, recordFormat,
                    recordLength, blockSize, sizeInTracks, spaceUnits, usedTracks, usedExtents, createDate, modDate,
                    expirationDate
                )
            }
        }
        return isValid
    }

    private fun isMemberPropertyValid(
        memName: String, version: String, createDate: String, modDate: String, modTime: String,
        userId: String, curRecNum: String, begRecNum: String, changedRecNum: String, remoteRobot: RemoteRobot,
    ): Boolean = with(remoteRobot) {
        var isValid = false
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            memberPropertiesDialog(fixtureStack) {
                isValid = areMemberPropertiesValid(
                    memName, version, createDate, modDate, modTime, userId, curRecNum, begRecNum, changedRecNum
                )
            }
        }
        return isValid
    }
    private fun isUssFilePropertyValid(
        fileName: String, location: String, path: String, size: String, modDate: String,
        owner: String, group: String, groupId: String, ownerPerm: String, groupPerm: String,
        allPerm: String, remoteRobot: RemoteRobot,
    ): Boolean = with(remoteRobot) {
        var isValid = false
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            ussFilePropertiesDialog(fixtureStack) {
                isValid = areUssFilePropertiesValid(
                    fileName, location, path, size, modDate, owner, group, groupId, ownerPerm, groupPerm, allPerm
                )
            }
        }
        return isValid
    }
}
