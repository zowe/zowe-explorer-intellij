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
 *   Uladzislau Kalesnikau
 */

package tests.utils

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.ui
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
import java.nio.file.*
import kotlin.time.Duration.Companion.minutes

/** IDE run manager. Provides functionalities to control IDE run process */
class IdeRunManager private constructor() {
  private val ideVersion by lazy { System.getProperty("ide.test.version") }
  private val pluginPathStr by lazy { System.getProperty("plugin.path") }
  private val mockProjectRelativePathStr by lazy { System.getProperty("ui.test.mock.project.path") }
  private val testCaseDesc by lazy {
    TestCase(IdeProductProvider.IC, LocalProjectInfo(Paths.get(mockProjectRelativePathStr)))
  }
  private val testContext: IDETestContext = Starter
    .newContext("test_plugin_action", testCase = testCaseDesc.useRelease(ideVersion))
    .prepareProjectCleanImport()
    .disableAutoImport(disabled = true)

  val runningIde: BackgroundRun

  companion object {
    private val createdRunManager by lazy { IdeRunManager() }

    private var isIDEAlreadyClosed = false

    /**
     * Prepare the IDE run manager instance.
     * Will close the starting Privacy Policy dialog and initialize an IDE.
     * If the initialization is already done, will just return the IDE run manager instance
     */
    fun prepareRunManager(): IdeRunManager {
      assert(!isIDEAlreadyClosed) { "IDE is already closed" }
      createdRunManager.runningIde.driver.waitForIndicators(5.minutes)
      return createdRunManager
    }

    /** Close the running IDE after tests are completed */
    fun closeIdeAfterTests() {
      assert(!isIDEAlreadyClosed) { "IDE is already closed" }
      prepareRunManager().closeIde()
      isIDEAlreadyClosed = true
    }

    /** Get the running IDE driver */
    fun getIdeDriver(): Driver {
      assert(!isIDEAlreadyClosed) { "IDE is already closed" }
      return prepareRunManager().runningIde.driver
    }

    /**
     * Take current IDE state screenshot.
     * It makes a fullscreen screenshot and places the screenshot under a reports folder
     * @param outFolder the folder to place the screenshot initially in IDEA test context folder
     * @param savePath the new screenshot path after it is stored under the test context path
     */
    fun takeCurrentIDEStateScreenshot(outFolder: String, savePath: Path) {
      assert(!isIDEAlreadyClosed) { "IDE is already closed" }
      prepareRunManager().runningIde.driver.takeScreenshot(outFolder)
      val screenshotPath = prepareRunManager()
        .testContext
        .paths
        .testHome
        .resolve("log")
        .resolve("screenshots")
        .resolve(outFolder)
        .resolve("full_screen.png")
      Files.copy(screenshotPath, savePath, StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * Dump IDE XPath tree as an HTML doc
     * @param folderPath the folder to place the dumped tree
     * @param fileName the file name to assign to the tree
     */
    fun dumpIDEXPathTree(folderPath: Path, fileName: String) {
      assert(!isIDEAlreadyClosed) { "IDE is already closed" }
      prepareRunManager().runningIde.driver.ui.robotProvider.saveHierarchy(folderPath.toString(), fileName)
    }
  }

  init {
    testContext.pluginConfigurator.installPluginFromPath(Paths.get(pluginPathStr))
    runningIde = testContext.runIdeWithConfiguredDriver()
  }

  /** Prepare the IDE and the driver for further usage */
  private fun IDETestContext.runIdeWithConfiguredDriver(): BackgroundRun {
    val commands = CommandChain().waitForDumbMode(20)

    return this.runIdeWithDriver(
      commands = commands,
      commandLine = {
        IDECommandLine.OpenTestCaseProject(this)
      },
      runTimeout = 60.minutes,
      configure = {
        addVMOptionsPatch {
          clearSystemProperty("ide.performance.screenshot")
        }
      }
    )
  }

  /** Close the running IDE */
  fun closeIde() {
    runningIde.closeIdeAndWait()
  }

}
