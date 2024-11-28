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

import auxiliary.*
import com.intellij.driver.sdk.ui.components.ideFrame
import com.intellij.driver.sdk.ui.components.isDialogOpened
import com.intellij.driver.sdk.ui.components.waitForNoOpenedDialogs
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.jediterm.core.input.KeyEvent
import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.actionButton
import org.junit.jupiter.api.TestInfo
import tests.utils.ActionMenuPoints
import tests.utils.FilesExplorerPanel
import tests.utils.dialogs.AddConnectionDialog
import tests.utils.dialogs.AddWorkingSetDialog
import tests.utils.notification.CreateConnectionErrorNotification
import tests.utils.notification.UnsecureConnectionDialog
import workingset.testutils.injectTestInfo
import workingset.testutils.injectTestInfoRestTopology

/**
 * Reset the running IDE test environment.
 * Is useful to reset the IDE state before the other tests run
 */
fun BackgroundRun.resetTestEnv(): BackgroundRun {
  driver.ideFrame {
    while (isDialogOpened()) {
      robot.pressAndReleaseKey(KeyEvent.VK_ESCAPE)
    }
    waitForNoOpenedDialogs()
  }
  return this
}

fun callRightSidePanel(driver: Driver) {
  driver.ideFrame {
    val forMainframeTool = rightToolWindowToolbar.actionButton {
      byAttribute("myaction", "For Mainframe (null)")
    }
    forMainframeTool.setFocus()
    forMainframeTool.click()
  }
}

/**
 * Creates valid connection to mock server.
 */
fun createValidConnectionWithMock(testInfo: TestInfo, connectionName: String, ideDriver: Driver) {
  injectTestInfo(testInfo)
  injectTestInfoRestTopology(testInfo)

  createConnection(
    connectionName,
    true,
    ideDriver,
    "https://${mockServer.hostName}:${mockServer.port}",
  )
}

/**
 * Steps to create a connection(valid or invalid) from settings .
 */

fun createConnection(
  connectionName: String,
  isValidConnection: Boolean,
  ideDriver: Driver,
  url: String = CONNECTION_URL,
  user: String = ZOS_USERID,
  password: String = ZOS_PWD,
  useSsl: Boolean = true
) {
  val filesExplorerPanel = FilesExplorerPanel(ideDriver)
  filesExplorerPanel.openExplorerToolWindow(ActionMenuPoints.CONNECTION)
  val addConnectionDialog = AddConnectionDialog(ideDriver)
  val unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
  if (isValidConnection) {
    addConnectionDialog.fillDialog(connectionName, url, user, password, useSsl)
  } else {
    addConnectionDialog.fillDialog(connectionName, "${url}1", user, password, useSsl)
  }
  unsecureConnectionDialog.preceedButton.click()
  addConnectionDialog.connectionDialogOkButton.click()
  unsecureConnectionDialog.preceedButton.click()
  if (isValidConnection.not()) {
    CreateConnectionErrorNotification(ideDriver).yesButton.click()
  }
}


/**
 * Creates working set without masks.
 */
fun createWsWithoutMask(
  ideDriver: Driver,
  wsName: String,
  connectionName: String,
) {
  FilesExplorerPanel(ideDriver).openExplorerToolWindow(ActionMenuPoints.WORKING_SET)
  val AddWorkingSetDialog = AddWorkingSetDialog(ideDriver)
  AddWorkingSetDialog.fillDialog(connectionName, wsName)
  AddWorkingSetDialog.okButton.click()
  AddWorkingSetDialog.okButton.click()
}

/**
 * Creates working set and a mask.
 */
fun createWsAndMask(ideDriver: Driver, wsName: String, connectionName: String, masks: List<Pair<String, String>>) {
  FilesExplorerPanel(ideDriver).openExplorerToolWindow(ActionMenuPoints.WORKING_SET)
  val AddWorkingSetDialog = AddWorkingSetDialog(ideDriver)
  AddWorkingSetDialog.fillDialog(connectionName, wsName, masks)
  AddWorkingSetDialog.okButton.click()

}
