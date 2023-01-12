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
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.event.KeyEvent

/**
 * Tests purging jobs and check results.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class PurgeJobTest {
    private var closableFixtureCollector = ClosableFixtureCollector()
    private var fixtureStack = mutableListOf<Locator>()
    private var wantToClose = mutableListOf(
        "Allocate Dataset Dialog"
    )
    private val projectName = "untitled"
    private val connectionName = "valid connection"

    private val wsName = "WS1"
    private val jwsName = "JWS1"
    private val datasetName = "$ZOS_USERID.PURGE.JOBS"

    private val filesList = mutableListOf("job_rc0.txt", "job_jcl_error.txt", "job_abend_s806.txt")
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/"

    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(projectName, fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        createWS(remoteRobot)
        allocatePDSAndCreateMask(wsName, datasetName, projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createJobs(remoteRobot)
        createJWS(remoteRobot)
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
    fun tearDown(remoteRobot: RemoteRobot) {
        closableFixtureCollector.closeWantedClosables(wantToClose, remoteRobot)
    }

    /**
     * Tests to purge jobs with RC=0, JCL ERROR, ABEND via action button in jobs terminal.
     * Checks that correct info is returned.
     */
    @Test
    @Order(1)
    fun testPurgeJobViaActionButtonInTerminal(remoteRobot: RemoteRobot) {
        for (i in 1..3) {
            doPurgeJobTestViaActionButton("TEST$i", remoteRobot)
        }
    }


    /**
     * Tests to purge jobs with RC=0, JCL ERROR, ABEND via context menu in Jes Explorer.
     * Checks that correct info is returned.
     */
    @Test
    @Order(2)
    fun testPurgeJobViaContextMenuInExplorer(remoteRobot: RemoteRobot) {
        for (i in 1..3) {
            doPurgeJobTestViaContextMenu("TEST$i", remoteRobot)
        }
    }

    /**
     * Creates working set.
     */
    private fun createJWS(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val filters = mutableListOf(
            Triple("TEST1", ZOS_USERID, ""),
            Triple("TEST2", ZOS_USERID, ""),
            Triple("TEST3", ZOS_USERID, "")
        )
        ideFrameImpl(projectName, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector,fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, filters)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
    }

    /**
     * Purges job via action button, checks notifications.
     */
    private fun doPurgeJobTestViaActionButton(jobName: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            submitJob(jobName, projectName, fixtureStack, remoteRobot)
            Thread.sleep(20000)
            val jobId = getJobIdFromPanel(remoteRobot)
            checkNotificationJobSubmitted(jobName, remoteRobot)
            purgeJobFromTerminal(remoteRobot)
            Thread.sleep(10000)
            checkNotificationJobPurged(jobName, jobId, true, remoteRobot)
            purgeJobFromTerminal(remoteRobot)
            Thread.sleep(10000)
            checkNotificationJobPurged(jobName, jobId, false, remoteRobot)
            closeTabInJobsPanel(jobName, remoteRobot)
        }

    /**
     * Purges job via context menu, checks notifications.
     */
    private fun doPurgeJobTestViaContextMenu(jobName: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            submitJob(jobName, projectName, fixtureStack, remoteRobot)
            Thread.sleep(20000)
            val jobId = getJobIdFromPanel(remoteRobot)
            checkNotificationJobSubmitted(jobName, remoteRobot)
            closeTabInJobsPanel(jobName, remoteRobot)
            purgeJobFromExplorer(jobName, jobId, remoteRobot)
            Thread.sleep(15000)
            checkNotificationJobPurged(jobName, jobId, true, remoteRobot)
            checkJobsOutputWasDeletedJWSRefreshed(jobName, jobId, remoteRobot)
            closeFilterInExplorer(Triple(jobName, ZOS_USERID, ""), projectName, fixtureStack, remoteRobot)
        }

    /**
     * Checks that job output was deleted after purging job.
     */
    private fun checkJobsOutputWasDeletedJWSRefreshed(
        jobName: String, jobId: String, remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                shouldThrow<NoSuchElementException> {
                    find<ComponentFixture>(viewTree).findText("$jobName ($jobId)")
                }
            }
        }
    }

    /**
     * Closes tab for job in jobs panel.
     */
    private fun closeTabInJobsPanel(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            find<ComponentFixture>(byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")).findText(
                "//'$datasetName($jobName)'"
            ).click()
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F4)
            }
        }
    }

    /**
     * Creates jobs in dataset.
     */
    private fun createJobs(remoteRobot: RemoteRobot) = with(remoteRobot) {
        var n = 1
        filesList.forEach {
            openLocalFileAndCopyContent(filePath + it, projectName, fixtureStack, remoteRobot)
            Thread.sleep(3000)
            createMemberAndPasteContent(datasetName, "TEST$n", projectName, fixtureStack, remoteRobot)
            n++
        }
    }

    /**
     * Creates working set.
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
     * Purges job via action button.
     */
    private fun purgeJobFromTerminal(remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            clickActionButton(byXpath("//div[@class='ActionButton' and @myaction='Purge Job ()']"))
        }
    }

    /**
     * Purges job via context menu.
     */
    private fun purgeJobFromExplorer(jobName: String, jobId: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                jesExplorer.click()
            }
        }
        openJobFilterInExplorer(Triple(jobName, ZOS_USERID, ""), "", projectName, fixtureStack, remoteRobot)
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith("$jobName ($jobId)") }.first()
                    .rightClick()
            }
            actionMenuItem(remoteRobot, "Purge Job").click()
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
     * Checks notification that correct info is returned for submitted job.
     */
    private fun checkNotificationJobSubmitted(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(projectName, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText("Job $jobName has been submitted")
                .click()
            find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
        }
    }

    /**
     * Checks notification that correct info is returned for purged job.
     */
    private fun checkNotificationJobPurged(
        jobName: String,
        jobId: String,
        jobExist: Boolean,
        remoteRobot: RemoteRobot
    ) = with(remoteRobot) {
        val textToFind = if (jobExist) {
            "$jobName: $jobId has been purged"
        } else {
            "Error purging $jobName: $jobId"
        }
        ideFrameImpl(projectName, fixtureStack) {
            find<JLabelFixture>(byXpath("//div[@javaclass='javax.swing.JLabel']")).findText(textToFind)
                .click()
            find<ComponentFixture>(byXpath("//div[@tooltiptext.key='tooltip.close.notification']")).click()
        }
    }
}