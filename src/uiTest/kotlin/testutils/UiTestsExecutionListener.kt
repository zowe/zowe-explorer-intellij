/*
 * Copyright (c) 2024 IBA Group.
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

package testutils

import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

/** Controls the environment preparation and correct reset before and after the regression tests are started */
class UiTestsExecutionListener : TestExecutionListener {
  /** Prepare an IDE before tests are run */
  override fun testPlanExecutionStarted(testPlan: TestPlan?) {
    super.testPlanExecutionStarted(testPlan)
    IdeRunManager.prepareRunManager()
  }

  /** Close the prepared IDE or just finish the execution if it is not initialized */
  override fun testPlanExecutionFinished(testPlan: TestPlan?) {
    IdeRunManager.closeIdeAfterTests()
    super.testPlanExecutionFinished(testPlan)
  }
}