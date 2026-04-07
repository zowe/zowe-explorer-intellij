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
import okhttp3.mockwebserver.MockResponse
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
    filesExplorerPanel = FilesExplorerPanel(ideDriver)
    addConnectionDialog = AddConnectionDialog(ideDriver)
    unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
    errorCreatingConnectionDialog = ErrorCreatingConnectionDialog(ideDriver)
  }

  @AfterEach
  fun finalizeTestEnv() {
    MockWebServerManager.removeAllEndpoints()
  }

  // Some helping instructions:
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

  /**
   * For Abdelrahman El Banna
   * @see
   * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#-add-valid-connection">
   *   Regression: Add valid connection
   * </a>
   */
  @Test
  @Tag("New")
  fun createValidConnectionTest(testInfo: TestInfo) {
    // Success scenario:
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
  }

  /**
   * For Andres Pedreros Castro
   * @see
   * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#-add-invalid-connection">
   *   Regression: Add invalid connection
   * </a>
   */
  @Test
  @Tag("New")
  fun createInvalidConnectionTest(testInfo: TestInfo) {
    // 401 Unauthorized (for wrong credentials):
    MockWebServerManager.injectEndpoint(
      "${testInfo.displayName}_info",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false },
      customHandler = {
        MockResponse().setResponseCode(401)
      }
    )
    // SSL certificate error (for self-signed SSL certificates when unchecked):
    MockWebServerManager.injectEndpoint(
      "${testInfo.displayName}_info",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false },
      customHandler = {
        MockResponse().setBody("Unable to find valid certification path to requested target")
      }
    )
  }

  /**
   * For Kalundi Serumaga
   * @see
   * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#-edit-existing-connection">
   *   Regression: Edit existing connection
   * </a>
   */
  @Test
  @Tag("New")
  fun editExistingConnectionTest(testInfo: TestInfo) {
    // Success scenario:
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

    // SSL certificate error (for self-signed SSL certificates when unchecked):
    MockWebServerManager.injectEndpoint(
      "${testInfo.displayName}_info",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false },
      customHandler = {
        MockResponse().setBody("Unable to find valid certification path to requested target")
      }
    )
  }
}
