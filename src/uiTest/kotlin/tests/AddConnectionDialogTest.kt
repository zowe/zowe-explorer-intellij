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

package tests

import auxiliary.mockServer
import auxiliary.startMockServer
import io.kotest.core.annotation.Description

import com.intellij.driver.client.Driver
import org.junit.jupiter.api.*
import tests.utils.ActionMenuPoints
import tests.utils.FilesExplorerPanel
import tests.utils.dialogs.AddConnectionDialog
import tests.utils.notification.UnsecureConnectionDialog
import testutils.*

import workingset.testutils.injectTestInfo
import workingset.testutils.injectTestInfoRestTopology

@Description("Tests for interaction and filling the connection creation dialog")
class AddConnectionDialogTest {
  private lateinit var ideDriver: Driver
  private lateinit var filesExplorerPanel: FilesExplorerPanel
  lateinit var addConnectionDialog: AddConnectionDialog
  private lateinit var unsecureConnectionDialog: UnsecureConnectionDialog

  companion object {
    @JvmStatic
    @BeforeAll
    fun prepareBeforeAll() {
      startMockServer()
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()
      val ideDriver = IdeRunManager.getIdeDriver()
      callRightSidePanel(ideDriver)
    }

    @JvmStatic
    @AfterAll
    fun afterAll() {
      mockServer.shutdown()
    }
  }


  @BeforeEach
  fun prepareTestEnv() {
    IdeRunManager.prepareRunManager()
      .runningIde
      .resetTestEnv()
    ideDriver = IdeRunManager.getIdeDriver()
    filesExplorerPanel = FilesExplorerPanel(ideDriver)
    addConnectionDialog = AddConnectionDialog(ideDriver)
    unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
  }

  @AfterEach
  fun finalizeTestEnv() {
    IdeRunManager.prepareRunManager()
      .runningIde
      .resetTestEnv()
  }

  @Test
  @Tag("New")
  fun fieldsPresenceTest() {
    filesExplorerPanel.openExplorerToolWindow(ActionMenuPoints.CONNECTION)

    addConnectionDialog.passwordInput.click()
    assert(addConnectionDialog.connectionDialogPanel.isVisible())
    assert(addConnectionDialog.connectionNameLabel.isVisible())
    assert(addConnectionDialog.connectionUrlLabel.isVisible())
    assert(addConnectionDialog.connectionUsernameLabel.isVisible())
    assert(addConnectionDialog.passwordLabel.isVisible())
    assert(addConnectionDialog.passwordInput.isVisible())
    assert(addConnectionDialog.questionMark.isVisible())
    assert(addConnectionDialog.connectionDialogCancelButton.isVisible())
  }

  @Test
  @Tag("New")
  fun createInvalidConnectionTest(testInfo: TestInfo) {
    injectTestInfo(testInfo)
    injectTestInfoRestTopology(testInfo)

    filesExplorerPanel.openExplorerToolWindow(ActionMenuPoints.CONNECTION)

    addConnectionDialog.passwordInput.text = "passwordInput"
    addConnectionDialog.connectionNameInput.text = "nameInput"
    addConnectionDialog.urlInput.text = "https://${mockServer.hostName}:${mockServer.port}"
    addConnectionDialog.userNameInput.text = "userNameInput"
    addConnectionDialog.acceptSelfSignedCheckbox.click()
    unsecureConnectionDialog.proceedButton.click()

    addConnectionDialog.connectionDialogOkButton.click()
    unsecureConnectionDialog.proceedButton.click()
  }
}
