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

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.actionButton
import com.intellij.driver.sdk.ui.components.dialog
import com.intellij.driver.sdk.ui.components.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.IDECommandLine
import com.intellij.ide.starter.runner.Starter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.waitForDumbMode
import java.nio.file.Paths
import kotlin.time.Duration.Companion.minutes

/** IDE run manager. Provides functionalities to control IDE run process */
class IdeRunManager private constructor() {
  private val ideVersion by lazy { System.getProperty("ide.test.version") }
  private val pluginPathStr by lazy { System.getProperty("plugin.path") }
  private val mockProjectRelativePathStr by lazy { System.getProperty("ui.tests.mock.project.path") }
  private val testCaseDesc by lazy {
    TestCase(IdeProductProvider.IC, LocalProjectInfo(Paths.get(mockProjectRelativePathStr)))
  }
  private val testContext: IDETestContext
  private var isPolicyDialogAlreadyClosed = false

  val runningIde: BackgroundRun

  companion object {
    private val createdRunManager by lazy { IdeRunManager() }

    /**
     * Prepare the IDE run manager instance.
     * Will close the starting Privacy Policy dialog and initialize an IDE.
     * If the initialization is already done, will just return the IDE run manager instance
     */
    fun prepareRunManager(): IdeRunManager {
      createdRunManager.runningIde
      if (!createdRunManager.isPolicyDialogAlreadyClosed) {
        createdRunManager.runningIde.driver.ideFrame {
          // Dismiss policy dialog
          val policyDialog = dialog(title = "For Mainframe Plugin Privacy Policy and Terms and Conditions")
          assert(policyDialog.isVisible())
          val dismissButton = policyDialog.actionButton { byText("Dismiss") }
          dismissButton.setFocus()
          dismissButton.click()

          createdRunManager.runningIde.driver.waitForIndicators(1.minutes)
        }
        createdRunManager.isPolicyDialogAlreadyClosed = true
      }
      return createdRunManager
    }

    /** Close the running IDE after tests are completed */
    fun closeIdeAfterTests() {
      prepareRunManager().closeIde()
    }

    /** Get the running IDE driver */
    fun getIdeDriver(): Driver {
      return prepareRunManager().runningIde.driver
    }
  }

  init {
    testCaseDesc.useRelease(ideVersion)
    testContext = Starter
      .newContext("test_plugin_action", testCase = testCaseDesc)
      .prepareProjectCleanImport()
      .disableAutoImport(disabled = true)
    testContext.pluginConfigurator.installPluginFromPath(Paths.get(pluginPathStr))
    runningIde = testContext.runIdeWithConfiguredDriver()
  }

  /** Prepare the IDE and the driver for further usage */
  private fun IDETestContext.runIdeWithConfiguredDriver(): BackgroundRun {
    val commands = CommandChain().waitForDumbMode(10)

    return this.runIdeWithDriver(
      commands = commands,
      commandLine = {
        IDECommandLine.OpenTestCaseProject(this)
      }
    )
  }

  /** Close the running IDE */
  fun closeIde() {
    runningIde.closeIdeAndWait()
  }

}