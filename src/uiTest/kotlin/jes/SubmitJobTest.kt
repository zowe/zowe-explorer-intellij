package jes

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.event.KeyEvent

/**
 * Tests submitting jobs and checks results.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class SubmitJobTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog"
    )
    private val projectName = "untitled"
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val datasetName = "$ZOS_USERID.SUBMIT.JOBS"

    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(projectName, fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        createWS(wsName, remoteRobot)
        allocateDataset(wsName, datasetName, remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        deleteDataset(datasetName, remoteRobot)
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
     * Tests to submit job with RC=0, checks that correct info is returned.
     */
    @Test
    @Order(1)
    fun testSubmitJobWithRC0(remoteRobot: RemoteRobot) {
        doSubmitJobTest("TEST1", "job_rc0.txt", "CC 0000", remoteRobot)
    }

    /**
     * Tests to submit job with JCL ERROR, checks that correct info is returned.
     */
    @Test
    @Order(2)
    fun testSubmitJobWithJclError(remoteRobot: RemoteRobot) {
        doSubmitJobTest("TEST2", "job_jcl_error.txt", "JCL ERROR", remoteRobot)
    }

    /**
     * Tests to submit job with ABEND, checks that correct info is returned.
     */
    @Test
    @Order(3)
    fun testSubmitJobWithAbend(remoteRobot: RemoteRobot) {
        doSubmitJobTest("TEST3", "job_abend_s806.txt", "ABEND S806", remoteRobot)
    }

    /**
     * Creates the member in dataset, submits it, checks that correct info is returned.
     */
    private fun doSubmitJobTest(jobName: String, fileName: String, rc: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            openFileAndCopyContent(fileName, remoteRobot)
            Thread.sleep(3000)
            createMemberAndPasteContent(datasetName, jobName, remoteRobot)
            submitJob(jobName, remoteRobot)
            Thread.sleep(20000)
            val jobId = getJobIdFromPanel(remoteRobot)
            checkNotification(jobName, remoteRobot)
            checkTabPanelAndConsole(jobName, jobId, rc, remoteRobot)
        }

    /**
     * Creates working set.
     */
    private fun createWS(wsName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            createWSFromContextMenu(fixtureStack, closableFixtureCollector)
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
                clickButton("OK")
                Thread.sleep(3000)
                find<HeavyWeightWindowFixture>(byXpath("//div[@class='HeavyWeightWindow']")).findText(
                    EMPTY_DATASET_MESSAGE
                )
                clickButton("OK")
                Thread.sleep(3000)
            }
            closableFixtureCollector.closeOnceIfExists(AddWorkingSetDialog.name)
        }
    }

    /**
     * Allocates a dataset and creates a mask for it.
     */
    private fun allocateDataset(wsName: String, datasetName: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    fileExplorer.click()
                    find<ComponentFixture>(viewTree).findText(wsName).rightClick()
                }
                actionMenu(remoteRobot, "New").click()
                actionMenuItem(remoteRobot, "Dataset").click()
                closableFixtureCollector.add(AllocateDatasetDialog.xPath(), fixtureStack)
                allocateDatasetDialog(fixtureStack) {
                    allocateDataset(datasetName, "PO", "TRK", 10, 1, 1, "VB", 255, 6120)
                    clickButton("OK")
                    Thread.sleep(10000)
                }
                closableFixtureCollector.closeOnceIfExists(AllocateDatasetDialog.name)
                find<ContainerFixture>(byXpath("//div[@class='MyDialog']")).findText("Dataset $datasetName Has Been Created")
                clickButton("Yes")
            }
            openOrCloseWorkingSetInExplorer(wsName, projectName, fixtureStack, remoteRobot)
        }

    /**
     * Creates a member in the dataset and pastes content to the member.
     */
    private fun createMemberAndPasteContent(
        datasetName: String,
        memberName: String,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                explorer {
                    find<ComponentFixture>(viewTree).findAllText(datasetName).last().rightClick()
                }
                actionMenu(remoteRobot, "New").click()
                actionMenuItem(remoteRobot, "Member").click()
                dialog("Create Member") {
                    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = memberName
                }
                clickButton("OK")
                Thread.sleep(10000)
                explorer {
                    find<ComponentFixture>(viewTree).findAllText(memberName).last().doubleClick()
                }
                with(textEditor()) {
                    keyboard {
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_V)
                        Thread.sleep(10000)
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_S)
                        Thread.sleep(10000)
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                    }
                }
            }
        }

    /**
     * Opens the file and copies it's content.
     */
    private fun openFileAndCopyContent(fileName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            actionMenu(remoteRobot, "File").click()
            runJs(
                """
            const point = new java.awt.Point(${locationOnScreen.x}, ${locationOnScreen.y});
            robot.moveMouse(component, point);
        """
            )
            actionMenuItem(remoteRobot, "Open...").click()
            Thread.sleep(3000)
            dialog("Open File or Project") {
                textField(byXpath("//div[@class='BorderlessTextField']")).text =
                    System.getProperty("user.dir") + "/src/uiTest/resources/$fileName"
                Thread.sleep(5000)
                clickButton("OK")
            }
            with(textEditor()) {
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_C)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                }
            }
        }
    }

    /**
     * Submits the job via context menu.
     */
    private fun submitJob(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findText(jobName).rightClick()
            }
            actionMenuItem(remoteRobot, "Submit Job").click()
        }
    }

    /**
     * Checks TabPanel and Console that correct info is returned.
     */
    private fun checkTabPanelAndConsole(jobName: String, jobId: String, rc: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            ideFrameImpl(projectName, fixtureStack) {
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "JOB $jobName($jobId) EXECUTED"
                )
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "OWNER: $ZOS_USERID"
                )
                find<ComponentFixture>(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).findText(
                    "RETURN CODE: $rc"
                )
                find<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).findText(
                    "//'$datasetName($jobName)'"
                ).click()
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
                }
            }
        }

    /**
     * Gets jobId for submitted job.
     */
    private fun getJobIdFromPanel(remoteRobot: RemoteRobot): String = with(remoteRobot) {
        var jobId = ""
        ideFrameImpl(projectName, fixtureStack) {
            jobId = find<ContainerFixture>(byXpath("//div[@class='Tree']")).findAllText()[2].text.trim()
        }
        return jobId
    }

    /**
     * Checks notification that correct info is returned.
     */
    private fun checkNotification(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText("Job $jobName has been submitted")
                .click()
            find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
        }
    }

    /**
     * Deletes dataset via context menu.
     */
    private fun deleteDataset(datasetName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findAllText(datasetName).last().rightClick()
            }
            actionMenuItem(remoteRobot, "Delete").click()
            dialog("Confirm Files Deletion") {
                clickButton("Yes")
            }
            Thread.sleep(10000)
        }
    }
}