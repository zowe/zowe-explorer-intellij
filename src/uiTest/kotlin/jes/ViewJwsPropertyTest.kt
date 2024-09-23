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

package jes

import auxiliary.*
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import testutils.ProcessManager
import workingset.*
import workingset.auxiliary.components.dialogs.JobPropertiesDialog
import workingset.auxiliary.components.dialogs.SpoolFilePropertyDialog
import workingset.testutils.*

/**
 * Tests viewing job and spool file properties.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class ViewJwsPropertyTest {
    private val closableFixtureCollector = ClosableFixtureCollector()
    private val fixtureStack = mutableListOf<Locator>()
    private var utilObject = IdeaInteractionClass()
    private val connectionName = "con1"
    private val jwsName = "JWS1"
    private val jobName = "TEST1"
    private val jesFileName = jobMemberCombo.format(jobName, UNIVERSAL_JOB_ID)
    private val jobsFilter = Triple(jobName, ZOS_USERID, "")


    private val spoolFileName = "JESMSGLG"
    private var jobPropertiesDialog = JobPropertiesDialog()
    private var spoolFilePropertyDialog = SpoolFilePropertyDialog()
    private var url = "https://%s:%s/zosmf/restjobs/jobs/$jobName/JOB07380"
    private var fileUrl = "https://%s:%s/zosmf/restjobs/jobs/$jobName/JOB07380/files"
    private var recUrk = "https://%s:%s/zosmf/restjobs/jobs/JOB07380S0W1....DB9E6D9D......."
    private var recUrkNonFormat = "%3A/files/2/records"
    private val generalTabProperty = mapOf(
            "jobId" to "JOB07380",
            "jobName" to jobName,
            "subsystem" to "<Unknown>",
            "owner" to ZOS_USERID.uppercase(),
            "status" to "OUTPUT",
            "jobType" to "JOB",
            "jobClass" to "A",
            "returnCode" to "CC 0000",
            "correlator" to "JOB07380S0W1....DB9E6D9D.......:",
    )
    private val dataTabProperty = mutableMapOf(
            "phase" to "0",
            "phaseName" to "PHASE1",
            "url" to url,
            "filesUrl" to fileUrl,
            "executor" to "SY1",
            "reasonNotRunning" to "<Unknown>",
            "submitted" to "2018-11-03T09:05:15.000Z",
            "startTime" to "2018-11-03T09:05:18.010Z",
            "endTime" to "2018-11-03T09:05:25.332Z"
    )
    private val generalSpoolTabProperty = mapOf(
            "jobId" to "JOB07380",
            "jobName" to jobName,
            "jobcorrelator" to "JOB07380S0W1....DB9E6D9D.......:",
            "class" to "K",
            "id" to "2",
            "dd" to spoolFileName,
            "step" to "STEP1",
            "process" to "<Unknown>",

    )
    private val dataSpoolTabProperty = mutableMapOf(
            "record format" to "UA",
            "byte content" to "0",
            "recocrd count" to "2",
            "record url" to recUrk,
            "length" to "133",
            "subsys" to "JES2"
    )
    private lateinit var processManager: ProcessManager


    /**
     * Opens the project and Explorer, clears test environment, creates jes working set with jobs filter.
     */
    @BeforeAll
    fun setUpAll(testInfo: TestInfo, remoteRobot: RemoteRobot) = with(remoteRobot) {
        processManager = ProcessManager()
        startMockServer()
        jobPropertiesDialog = JobPropertiesDialog(fixtureStack, remoteRobot)
        spoolFilePropertyDialog = SpoolFilePropertyDialog(fixtureStack, remoteRobot)

        injectTestInfo(testInfo)

        injectTestInfoRestTopology(testInfo)
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        createConnection(
            fixtureStack,
            closableFixtureCollector,
            connectionName,
            true,
            remoteRobot,
            "https://${mockServer.hostName}:${mockServer.port}"
        )
        dataTabProperty["url"] = url.format(mockServer.hostName, mockServer.port)
        dataTabProperty["filesUrl"] = fileUrl.format(mockServer.hostName, mockServer.port)
        dataSpoolTabProperty["record url"] = recUrk.format(mockServer.hostName, mockServer.port)+recUrkNonFormat

        injectEmptySpoolFiles(testInfo,jobName)
        injectOwnerPrefixJobDetails(testInfo,jobName,ZOS_USERID.uppercase())
        injectJobLogHttp(testInfo,jobName,GET_SINGLE_SPOOL_FILE)

        injectEmptySpoolFiles(testInfo,jobName)
        utilObject.createJWS(jwsName,connectionName,jobsFilter, fixtureStack, closableFixtureCollector, remoteRobot)
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
     * Test to check if job properties in opened dialog and expected values are matching
     */
    @Test
    fun testViewJobProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callTreesElementProperty(jesFileName,fixtureStack,remoteRobot, false)
        assertTrue(jobPropertiesDialog.checkTabParam(jobGeneralTabParams, generalTabProperty))
        find<ComponentFixture>(dataTabLoc).click()
        assertTrue(jobPropertiesDialog.checkTabParam(jobDataTabParams, dataTabProperty))
        jobPropertiesDialog.okButton.click()
    }

    /**
     * Test to check if spool file properties in opened dialog and expected values are matching
     */
    @Test
    fun testViewSpoolFileProperties(remoteRobot: RemoteRobot) = with(remoteRobot) {
        utilObject.callTreesElementProperty(spoolFileName,fixtureStack,remoteRobot, false)

        assertTrue(spoolFilePropertyDialog.checkTabParam(spoolGeneralTabParams, generalSpoolTabProperty ))
        find<ComponentFixture>(dataTabLoc).click()
        assertTrue(spoolFilePropertyDialog.checkTabParam(spoolDataTabParams, dataSpoolTabProperty))
        spoolFilePropertyDialog.okButton.click()
    }
}
