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
 *   Uladzislau Kalesnikau
 */

package tests

import auxiliary.mockServer
import io.kotest.core.annotation.Description
import com.intellij.driver.client.Driver
import org.junit.jupiter.api.*
import tests.utils.*
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.notification.UnsecureConnectionDialog
import tests.utils.uidefinitions.ActionMenuPoints
import tests.utils.uidefinitions.FilesExplorerPanel

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
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()
      val ideDriver = IdeRunManager.getIdeDriver()
      openZoweExplorerPanel(ideDriver)
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
    MockWebServerManager.removeAllEndpoints()
  }

  @Test
  @Tag("New")
  fun fieldsPresenceTest() {
    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)

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
    val mockServer = MockWebServerManager.prepareMockServer()

    MockWebServerManager.injectEndpoint(
      "${testInfo.displayName}_info",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
    )
    MockWebServerManager.injectEndpoint(
      "${testInfo.displayName}_resttopology",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
    )

    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)

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
