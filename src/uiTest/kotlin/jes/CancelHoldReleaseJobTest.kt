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
import auxiliary.components.stripeButton
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JLabelFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException
import com.intellij.remoterobot.utils.keyboard
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.event.KeyEvent

/**
 * Tests cancelling, holding, releasing jobs and checks results.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class CancelHoldReleaseJobTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog", "Add JES Working Set Dialog", "Add Working Set Dialog"
    )
    private val projectName = "untitled"
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val datasetName = "$ZOS_USERID.CANCEL.JOBS"
    private val jobName = "TEST1"
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/job_rc0.txt"

    private enum class JobAction { SUBMIT, CANCEL, HOLD, RELEASE }

    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(projectName, fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        createWS(remoteRobot)
        allocatePDSAndCreateMask(wsName, datasetName, projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createJob(remoteRobot)
    }

    /**
     * Closes the project and clears test environment.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        deleteDataset(datasetName, projectName, fixtureStack, remoteRobot)
        clearEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            close()
        }
    }

    /**
     * Closes all unclosed closable fixtures that we want to close.
     */
    @AfterEach
    fun tearDown(remoteRobot: RemoteRobot) = with(remoteRobot) {
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
        closeNotificationsAndJobsTabsIfExist(remoteRobot)
    }

    /**
     * Test to cancel one job and check that correct info is returned.
     */
    @Test
    @Order(1)
    fun testCancelOneJob(remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(7000)
        val jobId = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(10000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to release one job and check that correct info is returned.
     */
    @Test
    @Order(2)
    fun testHoldReleaseOneJob(remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId1 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId1, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId2 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId2, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(JobAction.HOLD, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId2, remoteRobot)
        doActionOnRunningJob(JobAction.RELEASE, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId2, remoteRobot)
        Thread.sleep(30000)
        checkJobResultInConsole(JobAction.SUBMIT, jobId2, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        checkJobResultInConsole(JobAction.SUBMIT, jobId1, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to cancel one job and check that correct info is returned.
     */
    @Test
    @Order(3)
    fun testHoldCancelOneJob(remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId1 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId1, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId2 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId2, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(JobAction.HOLD, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId2, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId2, remoteRobot)
        Thread.sleep(10000)
        checkJobResultInConsole(JobAction.CANCEL, jobId2, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        checkJobResultInConsole(JobAction.SUBMIT, jobId1, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to release, then to cancel one job and check that correct info is returned.
     */
    @Test
    @Order(4)
    fun testHoldReleaseCancelOneJob(remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId1 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId1, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId2 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId2, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(JobAction.HOLD, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId2, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(JobAction.RELEASE, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId2, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId2, remoteRobot)
        Thread.sleep(10000)
        checkJobResultInConsole(JobAction.CANCEL, jobId2, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        checkJobResultInConsole(JobAction.SUBMIT, jobId1, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to cancel two jobs and check that correct info is returned.
     */
    @Test
    @Order(5)
    fun testCancelTwoJobs(remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId1 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId1, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId2 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId2, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId3 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId3, remoteRobot)
        Thread.sleep(2000)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId3, remoteRobot)
        switchJobTabInJobsPanel(2, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId2, remoteRobot)
        switchJobTabInJobsPanel(3, remoteRobot)
        Thread.sleep(10000)
        checkJobResultInConsole(JobAction.CANCEL, jobId3, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId2, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        checkJobResultInConsole(JobAction.SUBMIT, jobId1, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to cancel two jobs and check that correct info is returned.
     */
    @Test
    @Order(6)
    fun testHoldCancelTwoJobs(remoteRobot: RemoteRobot) = with(remoteRobot) {
        editJob(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId1 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId1, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId2 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId2, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId3 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId3, remoteRobot)
        Thread.sleep(3000)

        doActionOnRunningJob(JobAction.HOLD, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId3, remoteRobot)

        switchJobTabInJobsPanel(2, remoteRobot)
        doActionOnRunningJob(JobAction.HOLD, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId2, remoteRobot)

        switchJobTabInJobsPanel(3, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId3, remoteRobot)

        switchJobTabInJobsPanel(2, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId2, remoteRobot)

        switchJobTabInJobsPanel(1, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId1, remoteRobot)

        switchJobTabInJobsPanel(3, remoteRobot)
        Thread.sleep(3000)
        checkJobResultInConsole(JobAction.CANCEL, jobId3, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        switchJobTabInJobsPanel(2, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId2, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId1, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Test to hold, then to release, then to cancel two jobs and check that correct info is returned.
     */
    @Test
    @Order(7)
    fun testHoldReleaseCancelTwoJobs(remoteRobot: RemoteRobot) = with(remoteRobot) {
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId1 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId1, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId2 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId2, remoteRobot)
        hideJobsPanel(remoteRobot)
        submitJob(jobName, projectName, fixtureStack, remoteRobot)
        Thread.sleep(5000)
        val jobId3 = getIdSubmittedJob(remoteRobot)
        checkNotificationAfterJobAction(JobAction.SUBMIT, jobId3, remoteRobot)
        Thread.sleep(3000)

        doActionOnRunningJob(JobAction.HOLD, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId3, remoteRobot)

        switchJobTabInJobsPanel(2, remoteRobot)
        doActionOnRunningJob(JobAction.HOLD, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.HOLD, jobId2, remoteRobot)

        switchJobTabInJobsPanel(3, remoteRobot)
        doActionOnRunningJob(JobAction.RELEASE, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId3, remoteRobot)

        switchJobTabInJobsPanel(2, remoteRobot)
        doActionOnRunningJob(JobAction.RELEASE, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.RELEASE, jobId2, remoteRobot)

        switchJobTabInJobsPanel(3, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId3, remoteRobot)

        switchJobTabInJobsPanel(2, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId2, remoteRobot)

        switchJobTabInJobsPanel(1, remoteRobot)
        doActionOnRunningJob(JobAction.CANCEL, remoteRobot)
        Thread.sleep(3000)
        checkNotificationAfterJobAction(JobAction.CANCEL, jobId1, remoteRobot)

        switchJobTabInJobsPanel(3, remoteRobot)
        Thread.sleep(3000)
        checkJobResultInConsole(JobAction.CANCEL, jobId3, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        switchJobTabInJobsPanel(2, remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId2, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
        checkJobResultInConsole(JobAction.CANCEL, jobId1, remoteRobot)
        closeJobTabInJobsPanel(remoteRobot)
    }

    /**
     * Closes notifications and jobs tabs in jobs panel if exists.
     */
    private fun closeNotificationsAndJobsTabsIfExist(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            try {
                find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']"))
                    .click()
                find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
            } catch (ex: Exception) {
                when (ex) {
                    !is WaitForConditionTimeoutException -> throw ex
                }
            }

            try {
                findAll<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).forEach {
                    it.click()
                    keyboard {
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
                    }
                }
            } catch (ex: Exception) {
                when (ex) {
                    !is WaitForConditionTimeoutException -> throw ex
                }
            }
        }
    }

    /**
     * Hides Jobs panel.
     */
    private fun hideJobsPanel(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            stripeButton(byXpath("//div[@class='StripeButton' and @text='Jobs']"))
                .click()
        }
    }

    /**
     * Gets jobId for submitted job.
     */
    private fun getIdSubmittedJob(remoteRobot: RemoteRobot): String = with(remoteRobot) {
        var jobId = ""
        ideFrameImpl(projectName, fixtureStack) {
            jobId = find<ContainerFixture>(byXpath("//div[@class='Tree']")).findAllText()[2].text.trim()
        }
        return jobId
    }

    /**
     * Checks jobs result.
     */
    private fun checkJobResultInConsole(action: JobAction, jobId: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            val rc = when (action) {
                JobAction.CANCEL -> "CANCELED"
                JobAction.SUBMIT -> "CC 0000"
                else -> throw Exception("Unknown action")
            }
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
            }
        }

    /**
     * Closes tab in jobs panel.
     */
    private fun closeJobTabInJobsPanel(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            findAll<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).last()
                .findText(
                    "//'$datasetName($jobName)'"
                ).click()
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
            }
        }
    }

    /**
     * Switches tab in jobs panel.
     */
    private fun switchJobTabInJobsPanel(jobOrder: Int, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            findAll<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']"))[jobOrder - 1].findText(
                "//'$datasetName($jobName)'"
            ).click()
        }
    }

    /**
     * Performs action (cancel/hold/release) on running job.
     */
    private fun doActionOnRunningJob(action: JobAction, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val myAction = when (action) {
            JobAction.CANCEL -> "Cancel Job ()"
            JobAction.HOLD -> "Hold Job ()"
            JobAction.RELEASE -> "Release Job ()"
            else -> throw Exception("Unknown action")
        }
        ideFrameImpl(projectName, fixtureStack) {
            clickActionButton(byXpath("//div[@class='ActionButton' and @myaction='$myAction']"))
        }
    }

    /**
     * Checks and closes notification after performed action on running job.
     */
    private fun checkNotificationAfterJobAction(
        action: JobAction,
        jobId: String,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val textToFind = when (action) {
            JobAction.CANCEL -> "$jobName: $jobId has been cancelled"
            JobAction.HOLD -> "$jobName: $jobId has been held"
            JobAction.RELEASE -> "$jobName: $jobId has been released"
            JobAction.SUBMIT -> "Job $jobName has been submitted"
        }
        ideFrameImpl(projectName, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText(textToFind)
                .click()
            find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
        }
    }

    /**
     * Creates empty working set.
     */
    private fun createWS(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
     * Creates job in dataset.
     */
    private fun createJob(remoteRobot: RemoteRobot) = with(remoteRobot) {
        openLocalFileAndCopyContent(filePath, projectName, fixtureStack, remoteRobot)
        Thread.sleep(3000)
        createMemberAndPasteContent(datasetName, jobName, projectName, fixtureStack, remoteRobot)
        editJob(remoteRobot)
    }

    /**
     * Edits the job.
     */
    private fun editJob(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                fileExplorer.click()
                find<ComponentFixture>(viewTree).findText(jobName).doubleClick()
            }
            with(textEditor()) {
                editor.findAllText { it.text.contains("sleep") }.first().click()
                keyboard {
                    key(KeyEvent.VK_END)
                    key(KeyEvent.VK_LEFT)
                    key(KeyEvent.VK_0)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_S)
                    Thread.sleep(2000)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                }
            }
        }
    }
}