package workingset

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.search.locators.Locator
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests viewing dataset and uss file properties.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ViewWsPropertyTest {
    private val closableFixtureCollector = ClosableFixtureCollector()
    private val fixtureStack = mutableListOf<Locator>()
    private val mapListDatasets = mutableMapOf<String, String>()
    private val mapListDatasetMembers = mutableMapOf<String, String>()
    private val mapListUssFiles = mutableMapOf<String, String>()

    private val projectName = "untitled"
    private val connectionName = "con1"
    private val wsName = "WS name"
    private val pdsName = "${ZOS_USERID.uppercase()}.UI.TEST"
    private val memberName = "TESTM"
    private val dsName = "${ZOS_USERID.uppercase()}.UI.TESTD"
    private val maskName = "${ZOS_USERID.uppercase()}.UI.TEST*"
    private val ussMaskName = "/u/${ZOS_USERID.uppercase()}"
    private val ussFileName = "testFile"
    private val ussDirName = "testFolder"

    private val memList =
        "{\"member\":\"$memberName\",\"vers\":1,\"mod\":0,\"c4date\":\"2015/08/12\",\"m4date\":\"2015/08/12\",\"cnorc\":22,\n" +
                "\"inorc\":22,\"mnorc\":0,\"mtime\":\"05:48\",\"msec\":\"43\",\"user\":\"IBMUSER\",\"sclm\":\"N\"},"
    private val fileList = "{\"name\":\"$ussFileName\",\"mode\":\"-rwxr--rw-\",\"size\":20,\"uid\":0,\"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
            "\"group\":\"OMVSGRP\",\"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirList = "{\"name\":\"$ussDirName\",\"mode\":\"drwxr--rw-\",\"size\":888, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\",\"gid\":1,\n" +
            "\"group\":\"OMVSGRP\",\"mtime\":\"2013-05-07T11:23:08\"},"
    private val dirHereList = "{\"name\":\".\", \"mode\":\"drwxrwxrwx\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
            "\"group\":\"OMVSGRP\", \"mtime\":\"2015-11-24T02:12:04\"},"
    private val dirParentList = "{\"name\":\"..\", \"mode\":\"drwxr-xr-x\", \"size\":8192, \"uid\":0, \"user\":\"${ZOS_USERID.uppercase()}\", \"gid\":1, \n" +
            "\"group\":\"OMVSGRP\", \"mtime\":\"2015-09-15T02:38:29\"},"

    /**
     * Opens the project and Explorer, clears test environment, creates working set with dataset and uss masks,
     * allocates dataset and pds with member, creates uss file and directory.
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
            "listAllAllocatedDatasets_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${maskName}") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListDatasets, true)) }
        )
        responseDispatcher.injectEndpoint(
            "listAllDatasetMembers_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/ds/$pdsName/member") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListDatasetMembers, false)) }
        )
        mapListUssFiles["."] = dirHereList
        mapListUssFiles[".."] = dirParentList
        responseDispatcher.injectEndpoint(
            "listAllUssFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: false },
            { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
        )
        responseDispatcher.injectEndpoint(
            "allocateDatasetMember_restfiles",
            { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/$pdsName($memberName)") ?: false },
            { MockResponse().setResponseCode(204) }
        )
        responseDispatcher.injectEndpoint(
            "allocatePds_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/$pdsName") ?: false },
            { MockResponse().setBody("{\"dsorg\":\"PDS\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
        )
        responseDispatcher.injectEndpoint(
            "allocateDs_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/$dsName") ?: false },
            { MockResponse().setBody("{\"dsorg\":\"PS\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":0,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
        )
        responseDispatcher.injectEndpoint(
            "allocateUssFile_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs$ussMaskName/$ussFileName") ?: false },
            { MockResponse().setResponseCode(201) }
        )
        responseDispatcher.injectEndpoint(
            "allocateUssDir_restfiles",
            { it?.requestLine?.contains("POST /zosmf/restfiles/fs$ussMaskName/$ussDirName") ?: false },
            { MockResponse().setResponseCode(201) }
        )
        mapListDatasets[pdsName] = listDS(pdsName, "PDS", "PO")
        allocatePDSAndCreateMask(
            wsName,
            pdsName,
            projectName,
            fixtureStack,
            closableFixtureCollector,
            remoteRobot,
            maskName,
            directory = 2
        )
        ideFrameImpl(projectName, fixtureStack) {
            createMask(wsName, fixtureStack, closableFixtureCollector)
            createMaskDialog(fixtureStack) {
                createMask(Pair(ussMaskName, "USS"))
                clickButton("OK")
                Thread.sleep(3000)
            }
        }
        mapListDatasets[dsName] = listDS(dsName, "", "PS")
        allocateDataSet(wsName, dsName, projectName, fixtureStack, remoteRobot)
        mapListDatasetMembers[memberName] = memList
        allocateMemberForPDS(pdsName, memberName, projectName, fixtureStack, remoteRobot)
        createUssFile(ussMaskName, ussFileName,"File", projectName, fixtureStack, remoteRobot)
        mapListUssFiles[ussFileName] = fileList
        createUssFile(ussMaskName, ussDirName,"Directory", projectName, fixtureStack, remoteRobot)
        mapListUssFiles[ussDirName] = dirList
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(pdsName).doubleClick()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(ussMaskName).doubleClick()
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

        clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            close()
        }
    }

    /**
     * Test to check if member properties in opened dialog and expected values are matching
     */
    @Test
    @Order(1)
    fun testViewMemberProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(memberName).rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            memberPropertiesDialog(fixtureStack) {
                if (!areMemberPropertiesValid(memberName, "1.0", "2015/08/12", "2015/08/12",
                        "05:48", "IBMUSER", "22", "22", "0")) {
                    throw Exception("Properties in opened 'Member Properties' dialog are different from expected")
                }
                clickButton("OK")
            }
        }
    }

    /**
     * Test to check if dataset properties in opened dialog and expected values are matching
     */
    @Test
    @Order(2)
    fun testViewDatasetProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(dsName).rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            datasetPropertiesDialog(fixtureStack) {
                if (!areDatasetPropertiesValid(dsName, "", "TEST.CATALOG.MASTER", "TESTVOL",
                        "3390", "PS", "VB", "255", "3200", "10",
                        "TRACKS", "1", "1", "2021/11/15", "2021/11/17", "***None***")) {
                    throw Exception("Properties in opened 'Dataset Properties' dialog are different from expected")
                }
                clickButton("OK")
            }
        }
    }

    /**
     * Test to check if pds properties in opened dialog and expected values are matching
     */
    @Test
    @Order(3)
    fun testViewPdsProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(pdsName).rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            datasetPropertiesDialog(fixtureStack) {
                if (!areDatasetPropertiesValid(pdsName, "PDS", "TEST.CATALOG.MASTER", "TESTVOL",
                        "3390", "PO", "VB", "255", "3200", "10",
                        "TRACKS", "1", "1", "2021/11/15", "2021/11/17", "***None***")) {
                    throw Exception("Properties in opened 'Dataset Properties' dialog are different from expected")
                }
                clickButton("OK")
            }
        }
    }

    /**
     * Test to check if uss file properties in opened dialog and expected values are matching
     */
    @Test
    @Order(4)
    fun testViewUssFileProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(ussFileName).rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            ussFilePropertiesDialog(fixtureStack) {
                if (!areUssFilePropertiesValid(ussFileName, ussMaskName.drop(1), "$ussMaskName/$ussFileName", "20 bytes",
                        "2015-11-24T02:12:04", "USER", "OMVSGRP", "1", "READ_WRITE_EXECUTE", "READ", "READ_WRITE")) {
                    throw Exception("Properties in opened 'File Properties' dialog are different from expected")
                }
                clickButton("OK")
            }
        }
    }

    /**
     * Test to check if uss directory properties in opened dialog and expected values are matching
     */
    @Test
    @Order(5)
    fun testViewUssDirProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findText(ussDirName).rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            ussFilePropertiesDialog(fixtureStack) {
                if (!areUssFilePropertiesValid(ussDirName, ussMaskName.drop(1), "$ussMaskName/$ussDirName", "888 bytes",
                        "2013-05-07T11:23:08", "USER", "OMVSGRP", "1", "READ_WRITE_EXECUTE", "READ", "READ_WRITE")) {
                    throw Exception("Properties in opened 'Directory Properties' dialog are different from expected")
                }
                clickButton("OK")
            }
        }
    }
}