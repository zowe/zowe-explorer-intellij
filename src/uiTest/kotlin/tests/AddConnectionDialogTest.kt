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
import com.intellij.driver.sdk.ui.components.*
import tests.utils.*
import tests.utils.uidefinitions.ActionMenuPoints
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.uidefinitions.dialogs.UnsecureConnectionDialog
import tests.utils.uidefinitions.FilesExplorerPanel
import tests.utils.uidefinitions.dialogs.ErrorCreatingConnectionDialog

@Description("Tests for interaction and filling the connection creation dialog")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
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
      ideDriver = IdeRunManager.getIdeDriver()
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
    // Close any open dialogs to clean up for the next test
    ideDriver.ideFrame {
      while (isDialogOpened()) {
        dialog("//div[@class='MyDialog' and not(ancestor::div[@class='MyDialog'])]").setFocus()
        keyboard { escape() }
      }
      waitForNoOpenedDialogs()
    }
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
  @Disabled
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

  /**
   * Helper function to switch to JES Explorer view
   * @param driver the IDE driver to use
   */
  private fun switchToJesExplorerView(driver: Driver) {
    driver.ideFrame {
      val zoweExplorerTabs = x("//div[@class='ToolWindowHeader'][.//div[@class='BaseLabel' and @visible_text='Zowe Explorer']]")
      val jesExplorerTabPotentialElements = zoweExplorerTabs.xx { byText("JES Explorer") }.list()
      if (jesExplorerTabPotentialElements.isNotEmpty()) {
        val jesExplorerTab = jesExplorerTabPotentialElements[0]
        jesExplorerTab.setFocus()
        jesExplorerTab.click()
      } else {
        val chevronWithOtherTabs = zoweExplorerTabs
          .actionButtonByXpath("//div[@class='ActionButton' and contains(@myicon,'chevron')]")
        chevronWithOtherTabs.setFocus()
        chevronWithOtherTabs.click()
        val jesExplorerTabPopup = popup()
        val jesExplorerTabPopupList = jesExplorerTabPopup.list("//div[@class='MyList']")
        jesExplorerTabPopupList.clickItemAtIndex(0)
      }
    }
  }

  /**
   * Helper function to switch to Files Explorer view
   * @param driver the IDE driver to use
   */
  private fun switchToFilesExplorerView(driver: Driver) {
    driver.ideFrame {
      val zoweExplorerTabs = x("//div[@class='ToolWindowHeader'][.//div[@class='BaseLabel' and @visible_text='Zowe Explorer']]")
      val fileExplorerTab = zoweExplorerTabs.x { byText("File Explorer") }
      fileExplorerTab.setFocus()
      fileExplorerTab.click()
    }
  }

  /**
   * Helper function to check connection exists in settings
   * @param driver the IDE driver to use
   * @param connectionName the connection name to check for
   */
  private fun checkConnectionExistsInSettings(driver: Driver, connectionName: String) {
    val settingsDialog = goToSettings(driver, "Connections")
    driver.ideFrame {
      val zoweExplorerSettingsTabs = settingsDialog.x {
        and(byClass("JBEditorTabs"), byAttribute("nextaction", "Select Next Tab (Activate next tab)"))
      }
      val tabElements = zoweExplorerSettingsTabs.table { byClass("ValidatingTableView") }
      assert(tabElements.isVisible())
      val tableText = tabElements.allTextAsString()
      assert(tableText.contains(connectionName)) { "Connection '$connectionName' should be in the settings list" }

      // Close settings dialog
      val cancelButton = settingsDialog.actionButton { and(byClass("JButton"), byVisibleText("Cancel")) }
      cancelButton.setFocus()
      cancelButton.click()
    }
  }

  /**
   * Case 1: Add invalid connection in Files Explorer view with unreachable URL
   * - Connection name: test_invalid_connection_1
   * - Connection URL: https://test.com:10444
   * - Username: test
   * - Password: test
   * Expected: Error dialog appears, clicking "Yes" adds connection to settings
   */
  @Test
  @Tag("New")
  @Order(1)
  fun createInvalidConnectionCase1FilesExplorerUnreachableUrl(testInfo: TestInfo) {
    val connectionName = "test_invalid_connection_1"
    val connectionUrl = "https://test.com:10444"
    val username = "test"
    val password = "test"

    // Part 1: Open connection dialog and fill values
    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)

    // Part 1 checks: Check that the connection dialog appears and text is correct
    assert(addConnectionDialog.dialogComponent.isVisible()) { "Add Connection dialog should be visible" }
    addConnectionDialog.checkFieldsArePresent()

    // Fill dialog with values
    addConnectionDialog.fillDialog(
      connectionName = connectionName,
      connectionUrl = connectionUrl,
      username = username,
      password = password,
      isAcceptSelfSigned = true
    )

    // Handle unsecure connection warning
    unsecureConnectionDialog.proceedButton.click()

    // Click OK
    addConnectionDialog.okButton.click()

    // Handle unsecure connection warning again
    unsecureConnectionDialog.proceedButton.click()

    // Wait for connection attempt
    Thread.sleep(5000)

    // Part 2: Check error dialog appears with correct text
    assert(errorCreatingConnectionDialog.dialogComponent.isVisible()) { "Error Creating Connection dialog should appear" }
    val errorDialogText = errorCreatingConnectionDialog.dialogComponent.allTextAsString()
    assert(errorDialogText.contains("Error Creating Connection")) { "Error dialog should have correct title" }

    // Click "Yes" to add the connection anyway
    errorCreatingConnectionDialog.yesButton.click()

    // Wait for the action to complete
    Thread.sleep(2000)

    // Part 2 checks: Check that a tooltip appears after adding invalid connection
    ideDriver.ideFrame {
    val tooltip = x("//div[@class='LimitedWidthJBHtmlPane' and contains(@visible_text, 'Now you can add working set')]")
    assert(tooltip.isVisible()) { "Tooltip should appear after adding invalid connection" }
    }

    // Part 3: Check connection is added in Settings
    checkConnectionExistsInSettings(ideDriver, connectionName)
  }

  /**
   * Case 2: Add invalid connection in JES Explorer view with unreachable URL
   * - Connection name: test_invalid_connection_2
   * - Connection URL: https://test.com:10444
   * - Username: test
   * - Password: test
   * Expected: Error dialog appears, clicking "Yes" adds connection to settings
   */
  @Test
  @Tag("New")
  @Order(2)
  fun createInvalidConnectionCase2JesExplorerUnreachableUrl(testInfo: TestInfo) {
    val connectionName = "test_invalid_connection_2"
    val connectionUrl = "https://test.com:10444"
    val username = "test"
    val password = "test"

    // Switch to JES Explorer view
    switchToJesExplorerView(ideDriver)

    // Open connection dialog from JES Explorer
    ideDriver.ideFrame {
      val jesExplorerView = x("//div[@class='SimpleToolWindowPanel' and div[@class='JesExplorerView']]")
      val plusButton = jesExplorerView.actionButton { byAttribute("myicon", "add.svg") }
      plusButton.click()
      val plusDropdownList = popup().list()
      plusDropdownList.clickItem("Connection")
    }

    // Reinitialize dialog components for JES Explorer context
    addConnectionDialog = AddConnectionDialog(ideDriver)
    unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
    errorCreatingConnectionDialog = ErrorCreatingConnectionDialog(ideDriver)

    // Check dialog appears
    assert(addConnectionDialog.dialogComponent.isVisible()) { "Add Connection dialog should be visible" }

    // Fill dialog with values
    addConnectionDialog.fillDialog(
      connectionName = connectionName,
      connectionUrl = connectionUrl,
      username = username,
      password = password,
      isAcceptSelfSigned = true
    )

    // Handle unsecure connection warning
    unsecureConnectionDialog.proceedButton.click()

    // Click OK
    addConnectionDialog.okButton.click()

    // Handle unsecure connection warning again
    unsecureConnectionDialog.proceedButton.click()

    // Wait for connection attempt
    Thread.sleep(5000)

    // Check error dialog appears with correct text
    assert(errorCreatingConnectionDialog.dialogComponent.isVisible()) { "Error Creating Connection dialog should appear" }
    val errorDialogText = errorCreatingConnectionDialog.dialogComponent.allTextAsString()
    assert(errorDialogText.contains("Error Creating Connection")) { "Error dialog should have correct title" }

    // Click "Yes" to add the connection anyway
    errorCreatingConnectionDialog.yesButton.click()

    // Wait for the action to complete
    Thread.sleep(2000)

    // Check that a tooltip appears after adding invalid connection
    ideDriver.ideFrame {
      val tooltip = x("//div[@class='LimitedWidthJBHtmlPane' and contains(@visible_text, 'Now you can add working set')]")
      assert(tooltip.isVisible()) { "Tooltip should appear after adding invalid connection" }
    }

    // Check connection is added in Settings
    checkConnectionExistsInSettings(ideDriver, connectionName)

    // Switch back to Files Explorer for subsequent tests
    switchToFilesExplorerView(ideDriver)
  }

  /**
   * Case 3: Add invalid connection in Files Explorer view with mock server (invalid credentials)
   * - Connection name: test_invalid_connection_3
   * - Connection URL: https://127.0.0.1:<mock-server-port>
   * - Username: invalid
   * - Password: invalid
   * Expected: 401 error dialog appears, clicking "Yes" adds connection to settings
   */
  @Test
  @Tag("New")
  @Order(3)
  fun createInvalidConnectionCase3FilesExplorerInvalidCredentials(testInfo: TestInfo) {
    val connectionName = "test_invalid_connection_3"
    val username = "invalid"
    val password = "invalid"

    // Set up mock server to return 401 Unauthorized
    MockWebServerManager.injectEndpoint(
      "${testInfo.displayName}_info",
      endpointResolver = { it?.requestLine?.contains("zosmf/info") ?: false },
      customHandler = {
        MockResponse().setResponseCode(401).setBody("Unauthorized")
      }
    )

    val mockServerUrl = MockWebServerManager.url

    // Open connection dialog
    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)

    // Check dialog appears
    assert(addConnectionDialog.dialogComponent.isVisible()) { "Add Connection dialog should be visible" }

    // Fill dialog with values
    addConnectionDialog.fillDialog(
      connectionName = connectionName,
      connectionUrl = mockServerUrl,
      username = username,
      password = password,
      isAcceptSelfSigned = true
    )

    // Handle unsecure connection warning
    unsecureConnectionDialog.proceedButton.click()

    // Click OK
    addConnectionDialog.okButton.click()

    // Handle unsecure connection warning again
    unsecureConnectionDialog.proceedButton.click()

    // Wait for connection attempt
    Thread.sleep(3000)

    // Check error dialog appears with correct text
    assert(errorCreatingConnectionDialog.dialogComponent.isVisible()) { "Error Creating Connection dialog should appear for 401 error" }
    val errorDialogText = errorCreatingConnectionDialog.dialogComponent.allTextAsString()
    assert(errorDialogText.contains("Error Creating Connection")) { "Error dialog should have correct title" }

    // Click "Yes" to add the connection anyway
    errorCreatingConnectionDialog.yesButton.click()

    // Wait for the action to complete
    Thread.sleep(2000)

    // Check connection is added in Settings
    checkConnectionExistsInSettings(ideDriver, connectionName)
  }

  /**
   * Case 4: Add invalid connection with SSL certificate error (self-signed unchecked)
   * - Connection name: test_invalid_connection_4
   * - Connection URL: https://127.0.0.1:<mock-server-port>
   * - Username: sslerr
   * - Password: test
   * - Accept self-signed SSL certificates: unchecked
   * Expected: SSL error dialog appears, clicking "Yes" adds connection to settings
   */
  @Test
  @Tag("New")
  @Order(4)
  fun createInvalidConnectionCase4SslCertificateError(testInfo: TestInfo) {
    val connectionName = "test_invalid_connection_4"
    val username = "sslerr"
    val password = "test"

    val mockServerUrl = MockWebServerManager.url

    // Open connection dialog
    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)

    // Check dialog appears
    assert(addConnectionDialog.dialogComponent.isVisible()) { "Add Connection dialog should be visible" }

    // Fill dialog with values - Note: isAcceptSelfSigned = false to trigger SSL error
    addConnectionDialog.fillDialog(
      connectionName = connectionName,
      connectionUrl = mockServerUrl,
      username = username,
      password = password,
      isAcceptSelfSigned = false
    )

    // Click OK (no unsecure dialog when self-signed is not checked)
    addConnectionDialog.okButton.click()

    // Wait for connection attempt - SSL error should occur
    Thread.sleep(5000)

    // Check error dialog appears with correct text
    assert(errorCreatingConnectionDialog.dialogComponent.isVisible()) { "Error Creating Connection dialog should appear for SSL error" }
    val errorDialogText = errorCreatingConnectionDialog.dialogComponent.allTextAsString()
    assert(errorDialogText.contains("Error Creating Connection")) { "Error dialog should have correct title" }

    // Click "Yes" to add the connection anyway
    errorCreatingConnectionDialog.yesButton.click()

    // Wait for the action to complete
    Thread.sleep(2000)

    // Check connection is added in Settings
    checkConnectionExistsInSettings(ideDriver, connectionName)
  }

  /**
   * Case 5: Add invalid connection from Settings dialog with invalid URL format
   * - Connection name: test_invalid_connection_5
   * - Connection URL: zzz
   * - Username: test
   * - Password: test
   * Expected: Error tooltip appears for invalid URL format
   */
  @Test
  @Tag("New")
  @Order(5)
  fun createInvalidConnectionCase5SettingsInvalidUrlFormat(testInfo: TestInfo) {
    val connectionName = "test_invalid_connection_5"
    val invalidUrl = "zzz"
    val username = "test"
    val password = "test"

    // Go to Settings and open Connections tab
    val settingsDialog = goToSettings(ideDriver, "Connections")

    ideDriver.ideFrame {
      val zoweExplorerSettingsTabs = settingsDialog.x {
        and(byClass("JBEditorTabs"), byAttribute("nextaction", "Select Next Tab (Activate next tab)"))
      }

      // Find the tab content with the plus button
      val tabContents = zoweExplorerSettingsTabs.x(
        "//div[@class='DialogPanel' and div[@class='TitledSeparator' and @originaltext='z/OSMF Connections']]"
      )
      val tabActions = tabContents.x { byClass("CommonActionsPanel") }
      val plusButton = tabActions.actionButton {
        and(byClass("ActionButton"), byAttribute("myaction", "Add (Add)"))
      }

      // Click plus button to open Add Connection dialog
      plusButton.setFocus()
      plusButton.click()

      // Get the Add Connection dialog from settings
      val addConnectionDialogFromSettings = dialog(title = "Add Connection")
      assert(addConnectionDialogFromSettings.isVisible()) { "Add Connection dialog should be visible from Settings" }

      // Fill the dialog with invalid URL
      val connectionDialogPanel = addConnectionDialogFromSettings.x { byClass("DialogPanel") }
      val inputFields = connectionDialogPanel.xx { byClass("JBTextField") }.list()
      val connectionNameInput = inputFields[0].textField()
      val urlInput = inputFields[1].textField()
      val userNameInput = inputFields[2].textField()
      val passwordInput = addConnectionDialogFromSettings.textField { byClass("JBPasswordField") }

      connectionNameInput.text = connectionName
      urlInput.text = invalidUrl
      userNameInput.text = username
      passwordInput.text = password

      // Click OK
      val okButton = addConnectionDialogFromSettings.actionButton { byVisibleText("OK") }
      okButton.click()

      // Wait for validation
      Thread.sleep(2000)

      // Verify that validation blocked the submission:
      // The dialog should still be visible (validation prevented it from closing)
      assert(addConnectionDialogFromSettings.isVisible()) {
        "Add Connection dialog should remain open due to URL validation error"
      }

      // Verify the invalid URL is still in the field (form was not submitted)
      val urlInputAfterValidation = connectionDialogPanel.xx { byClass("JBTextField") }.list()[1].textField()
      assert(urlInputAfterValidation.text == invalidUrl) {
        "URL field should still contain the invalid value '$invalidUrl'"
      }

      // Close the Add Connection dialog first
      val cancelDialogButton = addConnectionDialogFromSettings.actionButton { byVisibleText("Cancel") }
      cancelDialogButton.setFocus()
      cancelDialogButton.click()

      // Close settings dialog
      val cancelSettingsButton = settingsDialog.actionButton { and(byClass("JButton"), byVisibleText("Cancel")) }
      cancelSettingsButton.setFocus()
      cancelSettingsButton.click()
    }
  }

  /**
   * Case 6: Add invalid connection with duplicate connection name
   * - Connection name: test_invalid_connection_1 (same as Case 1)
   * - Connection URL: https://127.0.0.1:<mock-server-port>
   * - Username: test
   * - Password: test
   * Expected: Error tooltip appears for duplicate connection name
   */
  @Test
  @Tag("New")
  @Order(6)
  fun createInvalidConnectionCase6DuplicateConnectionName(testInfo: TestInfo) {
    val connectionName = "test_invalid_connection_1" // Same name as Case 1
    val username = "test"
    val password = "test"

    val mockServerUrl = MockWebServerManager.url

    // Open connection dialog
    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)

    // Check dialog appears
    assert(addConnectionDialog.dialogComponent.isVisible()) { "Add Connection dialog should be visible" }

    // Fill dialog with duplicate connection name
    addConnectionDialog.fillDialog(
      connectionName = connectionName,
      connectionUrl = mockServerUrl,
      username = username,
      password = password,
      isAcceptSelfSigned = true
    )

    // Handle unsecure connection warning
    unsecureConnectionDialog.proceedButton.click()

    // Click OK
    addConnectionDialog.okButton.click()

    // Handle unsecure connection warning if it appears
    try {
      unsecureConnectionDialog.proceedButton.click()
    } catch (e: Exception) {
      // May not appear if connection name validation fails first
    }

    // Wait for validation
    Thread.sleep(2000)

    // Verify that validation blocked the submission:
    // The dialog should still be visible (validation prevented it from closing)
    assert(addConnectionDialog.dialogComponent.isVisible()) {
      "Dialog should remain open due to duplicate connection name validation error"
    }

    // Verify the duplicate connection name is still in the dialog (form was not submitted)
    val dialogText = addConnectionDialog.dialogComponent.allTextAsString()
    assert(dialogText.contains(connectionName)) {
      "Dialog should still contain the duplicate connection name '$connectionName'"
    }

    // Close the dialog
    addConnectionDialog.cancelButton.click()
  }

  /**
   * For Kalundi Serumaga
   * @see
   * <a href="https://github.com/zowe/zowe-explorer-intellij/wiki/Manual-and-automated-test-cases-consistency#-edit-existing-connection">
   *   Regression: Edit existing connection
   * </a>
   */
  @Disabled
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
