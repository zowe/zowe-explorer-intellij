/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   IBA Group
 */

package tests.utils

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Controls the environment preparation and correct reset before and after the regression tests are started */
class UiTestsExecutionListener : TestExecutionListener {
  /** Prepare an IDE before tests are run */
  override fun testPlanExecutionStarted(testPlan: TestPlan?) {
    super.testPlanExecutionStarted(testPlan)
    IdeRunManager.prepareRunManager()
  }

  /**
   * Gather all the necessary information for analysing purposes after a test is failed.
   * Will produce a screenshot right after the fail as well as the XPath tree dump
   */
  override fun executionFinished(testIdentifier: TestIdentifier?, testExecutionResult: TestExecutionResult?) {
    if (testExecutionResult?.status == TestExecutionResult.Status.FAILED) {
      val formatter = DateTimeFormatter.ofPattern("yyyy_MMM_dd_HH_mm_ss_z")
      val timestamp = ZonedDateTime.now().format(formatter)
      val reportsFolderPath = Paths.get(System.getProperty("user.dir"), "build", "reports")
      val screenshotPlacingPath = reportsFolderPath.resolve("full_screen_${timestamp}.png")
      IdeRunManager.takeCurrentIDEStateScreenshot("failure_screenshots", screenshotPlacingPath)
      IdeRunManager.dumpIDEXPathTree(folderPath = reportsFolderPath, fileName = "xpath_dump_${timestamp}.html")
    }
    super.executionFinished(testIdentifier, testExecutionResult)
  }

  /** Close the prepared IDE or just finish the execution if it is not initialized */
  override fun testPlanExecutionFinished(testPlan: TestPlan?) {
    IdeRunManager.closeIdeAfterTests()
    super.testPlanExecutionFinished(testPlan)
  }
}