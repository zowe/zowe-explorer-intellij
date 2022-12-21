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
    private val filePath = System.getProperty("user.dir") + "/src/uiTest/resources/"

    /**
     * Opens the project and Explorer, clears test environment, creates working set and dataset.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(projectName, fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(projectName, fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        createWsWithoutMask(projectName, wsName, connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        allocatePDSAndCreateMask(wsName, datasetName, projectName, fixtureStack, closableFixtureCollector, remoteRobot)
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
     * Tests to open and close jobs outputs in explorer.
     */
    @Test
    @Order(4)
    fun testOpenAndCloseJobsOutputsInExplorer(remoteRobot: RemoteRobot) = with(remoteRobot) {
        val jwsName = "JWS1"
        val jobName = "TEST1"
        val filter = Triple(jobName, ZOS_USERID, "")
        ideFrameImpl(projectName, fixtureStack) {
            explorer {
                jesExplorer.click()
                settings(closableFixtureCollector, fixtureStack)
            }
            settingsDialog(fixtureStack) {
                configurableEditor {
                    jesWorkingSetsTab.click()
                    addJWS(closableFixtureCollector, fixtureStack)
                }
                addJesWorkingSetDialog(fixtureStack) {
                    addJesWorkingSet(jwsName, connectionName, filter)
                    clickButton("OK")
                    Thread.sleep(5000)
                }
                closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
                clickButton("OK")
            }
            closableFixtureCollector.closeOnceIfExists(SettingsDialog.name)
        }
        Thread.sleep(20000)
        openJobsOutputs(jobName, remoteRobot)
    }

    /**
     * Opens and closes the jobs outputs.
     */
    private fun openJobsOutputs(jobName: String, remoteRobot: RemoteRobot) = with(remoteRobot) {
        val fileList = listOf("JESMSGLG", "JESJCL", "JESYSMSG", "SYSUT2", "SYSPRINT")
        ideFrameImpl(projectName, fixtureStack) {
            if (find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(jobName) }.size > 1) {
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(jobName) }.first().doubleClick()
            }

            Thread.sleep(10000)
            fileList.forEach { fileName ->
                find<ComponentFixture>(viewTree).findAllText { it.text.startsWith(fileName) }.first().doubleClick()
                with(textEditor()) {
                    keyboard {
                        Thread.sleep(3000)
                        hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_F4)
                    }
                }
            }
        }
    }

    /**
     * Creates the member in dataset, submits it, checks that correct info is returned.
     */
    private fun doSubmitJobTest(jobName: String, fileName: String, rc: String, remoteRobot: RemoteRobot) =
        with(remoteRobot) {
            openLocalFileAndCopyContent(filePath + fileName, projectName, fixtureStack, remoteRobot)
            Thread.sleep(3000)
            createMemberAndPasteContent(datasetName, jobName, projectName, fixtureStack, remoteRobot)
            submitJob(jobName, projectName, fixtureStack, remoteRobot)
            Thread.sleep(20000)
            val jobId = getJobIdFromPanel(remoteRobot)
            checkNotification(jobName, remoteRobot)
            checkTabPanelAndConsole(jobName, jobId, rc, remoteRobot)
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
}