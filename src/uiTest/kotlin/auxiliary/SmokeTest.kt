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

package auxiliary

import auxiliary.*
import testutils.*
import auxiliary.closable.ClosableFixtureCollector
import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.extension.ExtendWith
import auxiliary.containers.*
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.*
import workingset.PROJECT_NAME

/**
 * When adding UI tests to GitHub Actions pipeline, there is a need to first run dummy test, which
 * gets rid of tips and agrees to license agreement.
 */
@Tag("SmokeTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
class SmokeTest {
    private val wsName = "ws1"
    private val jwsName = "jws1"
    private val connectionName = "conName"
    private var fixtureStack = mutableListOf<Locator>()
    private var closableFixtureCollector = ClosableFixtureCollector()

    private val defaultZosMask = Pair("$ZOS_USERID.*","z/OS")
    private val defaultUssMask = Pair("/u","USS")
    private val defaultJobFilter = Triple("*", ZOS_USERID, "")

    /**
     * Opens the project and Explorer, clears test environment, creates working set and mask.
     */
    @BeforeAll
    fun setUpAll(remoteRobot: RemoteRobot) {
        setUpTestEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
    }

    /**
     * Closes the project and clears test environment, deletes created datasets.
     */
    @AfterAll
    fun tearDownAll(remoteRobot: RemoteRobot) = with(remoteRobot) {
        clearEnvironment(fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            close()
        }
    }

    @Test
    fun testBasics(remoteRobot: RemoteRobot) = with(remoteRobot) {
        createConnection(fixtureStack, closableFixtureCollector, connectionName, true, remoteRobot)
        createWsAndMask(wsName, listOf(defaultZosMask, defaultUssMask), connectionName, fixtureStack, closableFixtureCollector, remoteRobot)
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createJesWorkingSetFromActionButton(closableFixtureCollector, fixtureStack)
            addJesWorkingSetDialog(fixtureStack) {
                addJesWorkingSet(jwsName, connectionName, ZOS_USERID, defaultJobFilter)
                clickButton("OK")
                Thread.sleep(5000)
            }
            closableFixtureCollector.closeOnceIfExists(AddJesWorkingSetDialog.name)
        }
        openOrCloseWorkingSetInExplorer(wsName, fixtureStack, remoteRobot)
        openMaskInExplorer(defaultUssMask.first, "", fixtureStack, remoteRobot)
        openMaskInExplorer(defaultZosMask.first, "",fixtureStack, remoteRobot)
    }
}
