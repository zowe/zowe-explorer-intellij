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
 *   Kalundi Serumaga
 */

package tests

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*
import io.kotest.core.annotation.Description
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.*
import tests.utils.*
import tests.utils.uidefinitions.ActionMenuPoints
import tests.utils.uidefinitions.FilesExplorerPanel
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.uidefinitions.dialogs.EditConnectionDialog
import tests.utils.uidefinitions.dialogs.ErrorCreatingConnectionDialog
import tests.utils.uidefinitions.dialogs.UnsecureConnectionDialog

/**
 * Tests for editing created z/OSMF connections in the Zowe Explorer settings.
 *
 * Test scenarios verify:
 * - Editing connection name without triggering connection check
 * - Editing connection URL and handling invalid URLs
 * - Converting invalid connections to valid ones
 * - SSL certificate acceptance checkbox behavior
 *
 * Based on test cases defined in "Kalundi Serumaga Test Cases.pdf"
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Tag("New")
@Description("Edit created connection test scenarios")
class EditConnectionTest {

  private lateinit var filesExplorerPanel: FilesExplorerPanel
  private lateinit var addConnectionDialog: AddConnectionDialog
  private lateinit var unsecureConnectionDialog: UnsecureConnectionDialog
  private lateinit var errorCreatingConnectionDialog: ErrorCreatingConnectionDialog

  companion object {
    private lateinit var ideDriver: Driver

    const val VALID_CONNECTION_NAME = "test_valid_connection_1"
    const val INVALID_CONNECTION_NAME = "test_invalid_connection_1"
    const val VALID_CONNECTION_NAME_EDITED = "test_valid_connection_2"
    const val INVALID_CONNECTION_URL = "https://test1.com:10444"

    @JvmStatic
    @BeforeAll
    fun setup() {
      IdeRunManager.prepareRunManager()
        .runningIde
        .resetTestEnv()

      ideDriver = IdeRunManager.getIdeDriver()
      openZoweExplorerPanel(ideDriver)

      MockWebServerManager.prepareMockServer()
      MockWebServerManager.injectEndpoint(
        "setup_info",
        jsonMock = "infoResponse",
        endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
      )
      MockWebServerManager.injectEndpoint(
        "setup_resttopology",
        jsonMock = "infoResponse",
        endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
      )

      // Step 1: Create valid connection (mock server)
      // Fresh instances avoid stale weak references in the driver
      FilesExplorerPanel(ideDriver).openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)
      AddConnectionDialog(ideDriver).fillDialog(
        connectionName = VALID_CONNECTION_NAME,
        connectionUrl = MockWebServerManager.url,
        username = "test",
        password = "test",
        isAcceptSelfSigned = true
      )
      AddConnectionDialog(ideDriver).okButton.click()
      try {
        UnsecureConnectionDialog(ideDriver).proceedButton.click()
      } catch (_: Exception) { }
      Thread.sleep(3000)

      // Step 2: Create invalid connection (unreachable URL)
      FilesExplorerPanel(ideDriver).openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)
      AddConnectionDialog(ideDriver).fillDialog(
        connectionName = INVALID_CONNECTION_NAME,
        connectionUrl = "https://test.com:10443",
        username = "test",
        password = "test",
        isAcceptSelfSigned = true
      )
      AddConnectionDialog(ideDriver).okButton.click()
      try {
        UnsecureConnectionDialog(ideDriver).proceedButton.click()
      } catch (_: Exception) { }
      Thread.sleep(3000)

      // Step 3: Accept the error dialog for the invalid connection
      try {
        ErrorCreatingConnectionDialog(ideDriver).yesButton.click()
      } catch (_: Exception) { }

      Thread.sleep(2000)
    }

    @JvmStatic
    @AfterAll
    fun cleanup() {
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
   * Opens the Settings dialog on the Connections tab, double-clicks the row
   * containing [connectionName] to open the Edit Connection dialog.
   * Returns a Pair of (settingsDialog, editConnectionDialog).
   */
  private fun openEditConnectionDialog(connectionName: String): Pair<DialogUiComponent, EditConnectionDialog> {
    val settingsDialog = goToSettings(ideDriver, "Connections")
    ideDriver.ideFrame {
      val zoweExplorerSettingsTabs = settingsDialog.x {
        and(byClass("JBEditorTabs"), byAttribute("nextaction", "Select Next Tab (Activate next tab)"))
      }
      val tabElements = zoweExplorerSettingsTabs.table { byClass("ValidatingTableView") }
      tabElements.isVisible()

      val tableText = tabElements.allTextAsString()
      for (row in 0 until tabElements.rowCount()) {
        tabElements.clickCell(row, 0)
        val selectedText = tabElements.allTextAsString()
        if (selectedText.contains(connectionName)) {
          tabElements.clickCell(row, 0)
          break
        }
      }
    }
    Thread.sleep(1000)
    val editDialog = EditConnectionDialog(ideDriver)
    return settingsDialog to editDialog
  }

  /**
   * Clicks Apply then OK in the settings dialog.
   */
  private fun applyAndCloseSettings(settingsDialog: DialogUiComponent) {
    ideDriver.ideFrame {
      Thread.sleep(1000)
      val applyButton = settingsDialog.actionButton {
        and(byClass("JButton"), byVisibleText("Apply"))
      }
      applyButton.setFocus()
      applyButton.click()

      Thread.sleep(1000)

      val okButton = settingsDialog.actionButton {
        and(byClass("JButton"), byVisibleText("OK"))
      }
      okButton.setFocus()
      okButton.click()
    }
  }

  /**
   * Verifies the connections table in the settings dialog contains the expected names.
   */
  private fun verifyConnectionNames(settingsDialog: DialogUiComponent, expectedNames: List<String>) {
    ideDriver.ideFrame {
      val zoweExplorerSettingsTabs = settingsDialog.x {
        and(byClass("JBEditorTabs"), byAttribute("nextaction", "Select Next Tab (Activate next tab)"))
      }
      val tabElements = zoweExplorerSettingsTabs.table { byClass("ValidatingTableView") }
      tabElements.isVisible()

      val tableText = tabElements.allTextAsString()
      for (name in expectedNames) {
        Assertions.assertTrue(tableText.contains(name),
          "Connection '$name' should be present in the table. Table text: $tableText")
      }
    }
  }

  /**
   * Case 1: Edit valid connection's name in Settings window
   *
   * 1. Go to Settings via gear button
   * 2. Double-click to edit valid connection
   * 3. Change connection name to: test_valid_connection_2
   * 4. Click OK in Edit connection dialog
   * 5. Click Apply in Settings dialog
   *
   * Expected: Connection check should NOT happen after OK and Apply buttons are clicked
   */
  @Test
  @Order(1)
  @Tag("New")
  fun testEditConnectionName() {
    MockWebServerManager.injectEndpoint(
      "case1_info",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
    )
    MockWebServerManager.injectEndpoint(
      "case1_resttopology",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
    )

    val (settingsDialog, editDialog) = openEditConnectionDialog(VALID_CONNECTION_NAME)

    editDialog.setConnectionName(VALID_CONNECTION_NAME_EDITED)
    editDialog.okButton.click()
    Thread.sleep(1000)

    var errorDialogAppeared = false
    try {
      val errDialog = ErrorCreatingConnectionDialog(ideDriver)
      if (errDialog.dialogComponent.isVisible()) {
        errorDialogAppeared = true
      }
    } catch (_: Exception) { }
    Assertions.assertFalse(errorDialogAppeared,
      "Connection check should NOT happen when only the name is changed")

    verifyConnectionNames(settingsDialog, listOf(VALID_CONNECTION_NAME_EDITED, INVALID_CONNECTION_NAME))

    applyAndCloseSettings(settingsDialog)
  }

  /**
   * Case 2: Edit valid connection's URL to an invalid one
   *
   * 1. Go to Settings via gear button
   * 2. Edit valid connection's URL to: https://test1.com:10444
   * 3. Click OK — error dialog should appear
   * 4. Click Yes
   * 5. Click Apply
   *
   * Expected: Error dialog appears, connections list preserved after clicking Yes
   */
  @Test
  @Order(2)
  @Tag("New")
  fun testEditConnectionUrlToInvalid() {
    val (settingsDialog, editDialog) = openEditConnectionDialog(VALID_CONNECTION_NAME_EDITED)

    editDialog.setConnectionUrl(INVALID_CONNECTION_URL)
    editDialog.okButton.click()
    Thread.sleep(3000)

    try {
      val errDialog = ErrorCreatingConnectionDialog(ideDriver)
      Assertions.assertTrue(errDialog.dialogComponent.isVisible(),
        "Error Creating Connection dialog should appear for invalid URL")
      errDialog.yesButton.click()
    } catch (e: Exception) {
      Assertions.fail<Unit>("Error Creating Connection dialog did not appear: ${e.message}")
    }

    Thread.sleep(1000)

    verifyConnectionNames(settingsDialog, listOf(VALID_CONNECTION_NAME_EDITED, INVALID_CONNECTION_NAME))

    applyAndCloseSettings(settingsDialog)
  }

  /**
   * Case 3: Edit invalid connection to make it valid
   *
   * 1. Go to Settings via gear button
   * 2. Edit the invalid connection (test_invalid_connection_1)
   * 3. Change URL to the mock server URL
   * 4. Click OK
   * 5. Click Apply
   *
   * Expected: Connection check happens after OK, no error dialog, connection becomes valid
   */
  @Test
  @Order(3)
  @Tag("New")
  fun testEditInvalidConnectionToValid() {
    MockWebServerManager.injectEndpoint(
      "case3_info",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
    )
    MockWebServerManager.injectEndpoint(
      "case3_resttopology",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
    )

    val (settingsDialog, editDialog) = openEditConnectionDialog(INVALID_CONNECTION_NAME)

    editDialog.setConnectionUrl(MockWebServerManager.url)
    editDialog.okButton.click()
    Thread.sleep(3000)

    var errorDialogAppeared = false
    try {
      val errDialog = ErrorCreatingConnectionDialog(ideDriver)
      if (errDialog.dialogComponent.isVisible()) {
        errorDialogAppeared = true
        errDialog.noButton.click()
      }
    } catch (_: Exception) { }
    Assertions.assertFalse(errorDialogAppeared,
      "No error dialog should appear when changing to a valid URL")

    verifyConnectionNames(settingsDialog, listOf(VALID_CONNECTION_NAME_EDITED, INVALID_CONNECTION_NAME))

    applyAndCloseSettings(settingsDialog)
  }

  /**
   * Case 4: Uncheck "Accept self-signed SSL certificates"
   *
   * 1. Go to Settings via gear button
   * 2. Edit the valid connection (test_invalid_connection_1, now valid after Case 3)
   * 3. Uncheck "Accept self-signed SSL certificates"
   * 4. Click OK
   * 5. Error dialog appears — click Yes
   *
   * Expected: Connection check happens, error dialog appears, connections list not changed
   */
  @Test
  @Order(4)
  @Tag("New")
  fun testUncheckSSLCertificateAcceptance() {
    MockWebServerManager.injectEndpoint(
      "case4_info",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false },
      customHandler = {
        MockResponse().setBody("Unable to find valid certification path to requested target")
      }
    )

    val (settingsDialog, editDialog) = openEditConnectionDialog(INVALID_CONNECTION_NAME)

    if (editDialog.acceptSelfSignedCheckbox.isSelected()) {
      editDialog.acceptSelfSignedCheckbox.click()
    }
    editDialog.okButton.click()
    Thread.sleep(3000)

    try {
      val errDialog = ErrorCreatingConnectionDialog(ideDriver)
      Assertions.assertTrue(errDialog.dialogComponent.isVisible(),
        "Error dialog should appear when SSL self-signed is unchecked for a self-signed server")
      errDialog.yesButton.click()
    } catch (e: Exception) {
      Assertions.fail<Unit>("Error Creating Connection dialog did not appear: ${e.message}")
    }

    Thread.sleep(1000)

    verifyConnectionNames(settingsDialog, listOf(VALID_CONNECTION_NAME_EDITED, INVALID_CONNECTION_NAME))

    applyAndCloseSettings(settingsDialog)
  }

  /**
   * Case 5: Re-check "Accept self-signed SSL certificates"
   * Repeat Case 4 but mark the checkbox as checked and verify the connection is valid again.
   *
   * 1. Go to Settings via gear button
   * 2. Edit the connection (test_invalid_connection_1)
   * 3. Check "Accept self-signed SSL certificates"
   * 4. Click OK
   * 5. Click Apply
   *
   * Expected: Connection check happens, no error dialog, connection is valid again
   */
  @Test
  @Order(5)
  @Tag("New")
  fun testRecheckSSLCertificateAcceptance() {
    MockWebServerManager.injectEndpoint(
      "case5_info",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
    )
    MockWebServerManager.injectEndpoint(
      "case5_resttopology",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
    )

    val (settingsDialog, editDialog) = openEditConnectionDialog(INVALID_CONNECTION_NAME)

    if (!editDialog.acceptSelfSignedCheckbox.isSelected()) {
      editDialog.acceptSelfSignedCheckbox.click()
    }
    editDialog.okButton.click()
    Thread.sleep(3000)

    var errorDialogAppeared = false
    try {
      val errDialog = ErrorCreatingConnectionDialog(ideDriver)
      if (errDialog.dialogComponent.isVisible()) {
        errorDialogAppeared = true
        errDialog.noButton.click()
      }
    } catch (_: Exception) { }
    Assertions.assertFalse(errorDialogAppeared,
      "No error dialog should appear when SSL self-signed is re-checked")

    verifyConnectionNames(settingsDialog, listOf(VALID_CONNECTION_NAME_EDITED, INVALID_CONNECTION_NAME))

    applyAndCloseSettings(settingsDialog)
  }

  /**
   * Case 6: Edit the now-invalid connection (test_valid_connection_2) back to valid.
   * Its URL was changed to https://test1.com:10444 in Case 2.
   *
   * 1. Go to Settings via gear button
   * 2. Edit the connection (test_valid_connection_2)
   * 3. Change URL back to valid mock server URL
   * 4. Click OK
   * 5. Click Apply
   *
   * Expected: Connection check happens, no error dialog, connection is valid again
   */
  @Test
  @Order(6)
  @Tag("New")
  fun testEditConnectionBackToValid() {
    MockWebServerManager.injectEndpoint(
      "case6_info",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false }
    )
    MockWebServerManager.injectEndpoint(
      "case6_resttopology",
      jsonMock = "infoResponse",
      endpointResolver = { it?.requestLine?.contains("zosmf/resttopology/systems") ?: false }
    )

    val (settingsDialog, editDialog) = openEditConnectionDialog(VALID_CONNECTION_NAME_EDITED)

    editDialog.setConnectionUrl(MockWebServerManager.url)
    editDialog.okButton.click()
    Thread.sleep(3000)

    var errorDialogAppeared = false
    try {
      val errDialog = ErrorCreatingConnectionDialog(ideDriver)
      if (errDialog.dialogComponent.isVisible()) {
        errorDialogAppeared = true
        errDialog.noButton.click()
      }
    } catch (_: Exception) { }
    Assertions.assertFalse(errorDialogAppeared,
      "No error dialog should appear when changing back to valid URL")

    verifyConnectionNames(settingsDialog, listOf(VALID_CONNECTION_NAME_EDITED, INVALID_CONNECTION_NAME))

    applyAndCloseSettings(settingsDialog)
  }
}
