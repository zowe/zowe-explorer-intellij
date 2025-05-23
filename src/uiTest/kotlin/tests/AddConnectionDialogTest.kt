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

import io.kotest.core.annotation.Description
import com.intellij.driver.client.Driver
import org.junit.jupiter.api.*
import tests.utils.*
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.uidefinitions.dialogs.UnsecureConnectionDialog
import tests.utils.uidefinitions.FilesExplorerPanel
import tests.utils.uidefinitions.dialogs.ErrorCreatingConnectionDialog

@Disabled("This testsuite needs to be reworked")
@Description("Tests for interaction and filling the connection creation dialog")
class AddConnectionDialogTest {
  private lateinit var filesExplorerPanel: FilesExplorerPanel
  private lateinit var addConnectionDialog: AddConnectionDialog
  private lateinit var unsecureConnectionDialog: UnsecureConnectionDialog
  private lateinit var errorCreatingConnectionDialog: ErrorCreatingConnectionDialog

  companion object {
    private lateinit var ideDriver: Driver

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
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()
      deleteConfigEntities(ideDriver, "Connections")
      MockWebServerManager.removeAllEndpoints()
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
    errorCreatingConnectionDialog = ErrorCreatingConnectionDialog(ideDriver)
  }

  @AfterEach
  fun finalizeTestEnv() {
    MockWebServerManager.removeAllEndpoints()
  }

  /**
   * @see
   * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#add-invalid-connection">
   *   Regression: Add invalid connection
   * </a>
   */
  @Disabled("This testcase needs to be reworked")
  @Test
  @Tag("New")
  fun createInvalidConnectionTest(testInfo: TestInfo) {
//    TODO: finalize the check (the error dialog should appear)
//    val mockServer = MockWebServerManager.prepareMockServer()
//
//    MockWebServerManager.injectEndpoint(
//      "${testInfo.displayName}_info",
//      jsonMock = "infoResponse",
//      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
//    )
//    MockWebServerManager.injectEndpoint(
//      "${testInfo.displayName}_resttopology",
//      jsonMock = "infoResponse",
//      endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
//    )
//
//    filesExplorerPanel.createInvalidConnection(ideDriver, connectionName)
//    addConnectionDialog
//      .fillDialog(
//        connectionName = "nameInput",
//        connectionUrl = "https://${mockServer.hostName}:${mockServer.port}",
//        username = "userNameInput",
//        password = "passwordInput",
//        isAcceptSelfSigned = true
//      )
//
//    unsecureConnectionDialog.proceedButton.click()
//    addConnectionDialog.okButton.click()
//    unsecureConnectionDialog.proceedButton.click()

//    assert(errorCreatingConnectionDialog.dialogComponent.isVisible())
//
//    errorCreatingConnectionDialog.noButton.click()
  }
}
