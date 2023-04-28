package jes

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
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
 * Tests viewing job and spool file properties.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ViewJwsPropertyTest {
    private val closableFixtureCollector = ClosableFixtureCollector()
    private val fixtureStack = mutableListOf<Locator>()

    private val projectName = "untitled"
    private val connectionName = "con1"
    private val jwsName = "JWS name"
    private val jobsFilterName = Triple("*", ZOS_USERID.uppercase(), "")
    private val jobName = "jobName"
    private val spoolFileName = "JESMSGLG"

    /**
     * Opens the project and Explorer, clears test environment, creates jes working set with jobs filter.
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
        ideFrameImpl(projectName, fixtureStack) {
            createJWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName)
                clickButton("OK")
                Thread.sleep(2000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(2000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
            createJobsFilter(jwsName, fixtureStack, closableFixtureCollector)
            createJobsFilterDialog(fixtureStack) {
                createJobsFilter(jobsFilterName)
                clickButton("OK")
            }
        }
        responseDispatcher.injectEndpoint(
            "listJobs_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs?owner=USER&prefix=*") ?: false },
            { MockResponse().setBody(replaceInJson("getJob", mapOf(Pair("jobName", jobName), Pair("retCode", "CC 0000"),
                    Pair("jobStatus", "OUTPUT")))) })
        responseDispatcher.injectEndpoint(
            "listSpoolFiles_restfiles",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/") ?: false },
            { MockResponse().setBody(replaceInJson("getSingleSpoolFile", mapOf(Pair("jobName", jobName), Pair("spoolFileName", spoolFileName)))) })
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                jesExplorer.click()
                find<ComponentFixture>(viewTree).findText(jwsName).doubleClick()
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
     * Test to check if job properties in opened dialog and expected values are matching
     */
    @Test
    @Order(1)
    fun testViewJobProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findAllText{ it.text.startsWith("$jobName (JOB07380)") }.first()
                    .rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            jobPropertiesDialog(fixtureStack) {
                if (!areJobPropertiesValid("JOB07380", jobName, "", ZOS_USERID.uppercase(), "OUTPUT", "JOB",
                        "A", "CC 0000", "JOB07380S0W1....DB9E6D9D.......:", "0", "PHASE1",
                        "https://hostName:port/zosmf/restjobs/jobs/$jobName/JOB07380", "https://hostName:port/zosmf/restjobs/jobs/$jobName/JOB07380/files",
                        "SY1", "", "2018-11-03T09:05:15.000Z", "2018-11-03T09:05:18.010Z", "2018-11-03T09:05:25.332Z")) {
                    throw Exception("Properties in opened 'Job Properties' dialog are different from expected")
                }
                clickButton("OK")
            }
        }
    }

    /**
     * Test to check if spool file properties in opened dialog and expected values are matching
     */
    @Test
    @Order(2)
    fun testViewSpoolFileProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                jesExplorer.click()
                Thread.sleep(1000)
                find<ComponentFixture>(viewTree).findAllText{ it.text.startsWith(spoolFileName) }.first().rightClick()
            }
            actionMenuItem(remoteRobot, "Properties").click()
            spoolFilePropertiesDialog(fixtureStack) {
                if (!areSpoolFilePropertiesValid("JOB07380", jobName, "JOB07380S0W1....DB9E6D9D.......:", "K", "2", spoolFileName,
                        "STEP1", "", "UA", "0", "2",
                        "https://hostName:port/zosmf/restjobs/jobs/JOB07380S0W1....DB9E6D9D.......%3A/files/2/records", "133", "JES2")) {
                    throw Exception("Properties in opened 'Spool File Properties' dialog are different from expected")
                }
                clickButton("OK")
            }
        }
    }
}