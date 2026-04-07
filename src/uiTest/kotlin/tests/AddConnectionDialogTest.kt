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

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*
import io.kotest.core.annotation.Description
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import tests.utils.*
import tests.utils.uidefinitions.ActionMenuPoints
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.uidefinitions.FilesExplorerPanel
import tests.utils.uidefinitions.JesExplorerPanel

@Description("Tests for interaction and filling the connection creation dialog")
class AddConnectionDialogTest {
  companion object {
    private const val CONNECTIONS_TAB = "Connections"
    private const val TEST_USERNAME = "test"
    private const val TEST_USERNAME_2 = "test2"
    private const val TEST_PASSWORD = "test"

    private const val CASE_1_CONNECTION = "test_valid_connection_1"
    private const val CASE_2_CONNECTION = "test_valid_connection_2"
    private const val CASE_4_CONNECTION = "test_valid_connection_3"

    private lateinit var ideDriver: Driver

    @JvmStatic
    @BeforeAll
    fun prepareBeforeAll() {
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()
      ideDriver = IdeRunManager.getIdeDriver()
      openZoweExplorerPanel(ideDriver)
      deleteConfigEntities(ideDriver, CONNECTIONS_TAB)
    }

    @JvmStatic
    @AfterAll
    fun afterAll() {
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()
      deleteConfigEntities(ideDriver, CONNECTIONS_TAB)
      MockWebServerManager.removeAllEndpoints()
    }
  }

  @BeforeEach
  fun prepareTestEnv() {
    IdeRunManager.prepareRunManager()
      .runningIde
      .resetTestEnv()
  }

  @AfterEach
  fun finalizeTestEnv() {
    IdeRunManager.prepareRunManager()
      .runningIde
      .resetTestEnv()
    deleteConfigEntities(ideDriver, CONNECTIONS_TAB)
    MockWebServerManager.removeAllEndpoints()
  }

  private fun injectSuccessfulConnectionEndpoints(testInfo: TestInfo) {
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

  private fun assertConnectionsInSettings(
    expectedRowsCount: Int,
    expectedConnectionNames: List<String>,
    expectedUsernames: List<String>
  ) {
    val settingsDialog = goToSettings(ideDriver, CONNECTIONS_TAB)
    ideDriver.ideFrame {
      val connectionsTable = settingsDialog.table { byClass("ValidatingTableView") }
      assertEquals(expectedRowsCount, connectionsTable.rowCount())
      val tableText = settingsDialog.x { byClass("ValidatingTableView") }.allTextAsString()
      val normalizedTableText = tableText.uppercase()
      expectedConnectionNames.forEach { expectedName ->
        assertTrue(tableText.contains(expectedName), "Expected connection '$expectedName' to be present in Settings")
      }
      expectedUsernames.forEach { expectedUsername ->
        assertTrue(
          normalizedTableText.contains(expectedUsername.uppercase()),
          "Expected username '$expectedUsername' to be present in Settings"
        )
      }
      settingsDialog.actionButton { byVisibleText("OK") }.click()
    }
  }

  private fun assertSingleConnectionValuesInEditDialog(
    expectedConnectionName: String,
    expectedUrl: String,
    expectedUsername: String
  ) {
    val settingsDialog = goToSettings(ideDriver, CONNECTIONS_TAB)
    ideDriver.ideFrame {
      val connectionsTable = settingsDialog.table { byClass("ValidatingTableView") }
      assertEquals(1, connectionsTable.rowCount())
      connectionsTable.doubleClickCell(0, 0)

      val editConnectionDialog = AddConnectionDialog(ideDriver)
      assertEquals(expectedConnectionName, editConnectionDialog.getConnectionNameValue())
      assertEquals(expectedUrl, editConnectionDialog.getConnectionUrlValue())
      assertEquals(expectedUsername.uppercase(), editConnectionDialog.getUsernameValue().uppercase())
      editConnectionDialog.cancelButton.click()
      settingsDialog.actionButton { byVisibleText("Cancel") }.click()
    }
  }

  private fun deleteAllConnectionsInSettings() {
    IdeRunManager.prepareRunManager()
      .runningIde
      .resetTestEnv()
    deleteConfigEntities(ideDriver, CONNECTIONS_TAB)
  }

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
    injectSuccessfulConnectionEndpoints(testInfo)

    // Case 1: valid connection via Files Explorer.
    FilesExplorerPanel(ideDriver).createValidConnection(
      ideDriver = ideDriver,
      connectionName = CASE_1_CONNECTION,
      username = TEST_USERNAME,
      password = TEST_PASSWORD
    )
    assertConnectionsInSettings(
      expectedRowsCount = 1,
      expectedConnectionNames = listOf(CASE_1_CONNECTION),
      expectedUsernames = listOf(TEST_USERNAME)
    )
    deleteAllConnectionsInSettings()

    // Case 2: valid connection via JES Explorer with trailing spaces in some fields.
    val connectionUrlWithTrailingSpaces = "${MockWebServerManager.url}    "
    val jesExplorerPanel = JesExplorerPanel(ideDriver)
    jesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)
    val addConnectionDialog = AddConnectionDialog(ideDriver)
    addConnectionDialog.fillDialog(
      connectionName = "$CASE_2_CONNECTION    ",
      connectionUrl = connectionUrlWithTrailingSpaces,
      username = "$TEST_USERNAME    ",
      password = TEST_PASSWORD,
      isAcceptSelfSigned = false
    )
    addConnectionDialog.okButton.click()
    assertConnectionsInSettings(
      expectedRowsCount = 1,
      expectedConnectionNames = listOf(CASE_2_CONNECTION),
      expectedUsernames = listOf(TEST_USERNAME)
    )
    assertSingleConnectionValuesInEditDialog(
      expectedConnectionName = CASE_2_CONNECTION,
      expectedUrl = MockWebServerManager.url,
      expectedUsername = TEST_USERNAME
    )
    deleteAllConnectionsInSettings()

    // Case 3: valid connection with 200-character name.
    val longConnectionName = "B".repeat(200)
    FilesExplorerPanel(ideDriver).createValidConnection(
      ideDriver = ideDriver,
      connectionName = longConnectionName,
      username = TEST_USERNAME,
      password = TEST_PASSWORD
    )
    assertConnectionsInSettings(
      expectedRowsCount = 1,
      expectedConnectionNames = emptyList(),
      expectedUsernames = listOf(TEST_USERNAME)
    )
    assertSingleConnectionValuesInEditDialog(
      expectedConnectionName = longConnectionName,
      expectedUrl = MockWebServerManager.url,
      expectedUsername = TEST_USERNAME
    )
    deleteAllConnectionsInSettings()

    // Case 4: adding another connection with different username is allowed.
    val filesExplorerPanel = FilesExplorerPanel(ideDriver)
    filesExplorerPanel.createValidConnection(
      ideDriver = ideDriver,
      connectionName = CASE_1_CONNECTION,
      username = TEST_USERNAME,
      password = TEST_PASSWORD
    )
    filesExplorerPanel.createValidConnection(
      ideDriver = ideDriver,
      connectionName = CASE_4_CONNECTION,
      username = TEST_USERNAME_2,
      password = TEST_PASSWORD
    )
    assertConnectionsInSettings(
      expectedRowsCount = 2,
      expectedConnectionNames = listOf(CASE_1_CONNECTION, CASE_4_CONNECTION),
      expectedUsernames = listOf(TEST_USERNAME, TEST_USERNAME_2)
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
  @Disabled("Assigned to another scenario owner")
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
  @Disabled("Assigned to another scenario owner")
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
