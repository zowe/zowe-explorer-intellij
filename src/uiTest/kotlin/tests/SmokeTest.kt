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

import com.intellij.driver.sdk.ui.components.*
import io.kotest.core.annotation.Description
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import testutils.IdeRunManager
import testutils.resetTestEnv
import java.awt.Point

@Description("Smoke test case to check basic functionalities of the plug-in")
class SmokeTest {

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
  }

  /** Check Add Connection dialog elements are correct **/
  private fun checkConnectionDialog(connectionDialog: UiComponent) {
    // Check text fields
    val connectionDialogPanel = connectionDialog.x { byClass("DialogPanel") }
    assert(connectionDialogPanel.isVisible())
    val connectionNameLabel = connectionDialogPanel.x { and(byClass("JLabel"), byText("Connection name: ")) }
    assert(connectionNameLabel.isVisible())
    val connectionUrlLabel = connectionDialogPanel.x { and(byClass("JLabel"), byText("Connection URL: ")) }
    assert(connectionUrlLabel.isVisible())
    val connectionUsernameLabel = connectionDialogPanel.x { and(byClass("JLabel"), byText("Username: ")) }
    assert(connectionUsernameLabel.isVisible())
    val passwordLabel = connectionDialogPanel.x { and(byClass("JLabel"), byText("Password: ")) }
    assert(passwordLabel.isVisible())
    val acceptSelfSignedCheckbox = connectionDialogPanel.x {
      and(byClass("JBCheckBox"), byText("Accept self-signed SSL certificates"))
    }
    assert(acceptSelfSignedCheckbox.isVisible())

    // Check input fields
    val inputFields = connectionDialogPanel.xx { byClass("JBTextField") }
    assert(inputFields.list().size == 3)
    val passwordInput = connectionDialogPanel.x { byClass("JPasswordField") }
    assert(passwordInput.isVisible())

    // Check question mark
    val questionMark = connectionDialogPanel.x { byAttribute("defaulticon", "questionMark.svg") }
    assert(questionMark.isVisible())
    questionMark.setFocus()
    val questionMarkTip = connectionDialog.x { byClass("HeavyWeightWindow") }
    assert(questionMarkTip.isVisible())
    val questionMarkTipParagraph = questionMarkTip.x { byClass("Paragraph") }
    assert(questionMarkTipParagraph.isVisible())
    val questionMarkTipText = questionMarkTipParagraph.searchService.findAllText(questionMarkTipParagraph.component)
    val questionMarkTipHasText = questionMarkTipText.any { textLines ->
      textLines.text.contains("Select this checkbox")
    }
    val questionMarkTipHasCorrectText = questionMarkTipText.any { textLines ->
      textLines.text.contains("self-signed")
    }
    assert(questionMarkTipHasText && questionMarkTipHasCorrectText)

    val connectionDialogCancelButton = connectionDialog.actionButton { byVisibleText("Cancel") }
    assert(connectionDialogCancelButton.isVisible())
    connectionDialogCancelButton.setFocus()
    connectionDialogCancelButton.click()
  }

  /**
   * Check the selected settings tab basic elements if it is a table view tab
   * @param forMainframeSettingsTabs the settings tab to check
   * @param tabText the tab text to distinguish between different types of tabs
   * @return a plus button on the tab for further checking
   */
  private fun checkSettingsTabElements(forMainframeSettingsTabs: UiComponent, tabText: String): ActionButtonUi {
    val (titledSeparatorText, tabContentsHeaderText) = when (tabText) {
      "Connections" -> "z/OSMF Connections" to "Name || z/OSMF URL || Username || Owner"
      "JES Working Sets" -> "JES Working Sets" to "Name || Connection || Username || z/OSMF URL"
      "Working Sets" -> "Working Sets" to "Name || Connection || Username || z/OSMF URL"
      "TSO Sessions" -> "TSO Sessions" to "Name || Connection || Logon Procedure || Account Number"
      else -> "" to ""
    }

    // Check the tab content
    val tabContents = forMainframeSettingsTabs.x(
      "//div[@class='DialogPanel' and div[@class='TitledSeparator' and @originaltext='$titledSeparatorText']]"
    )
    assert(tabContents.isVisible())
    val tabContentsHeader = tabContents.x { and(byClass("JBTableHeader"), byVisibleText(tabContentsHeaderText)) }
    assert(tabContentsHeader.isVisible())
    val tabElements = tabContents.x { and(byClass("ValidatingTableView"), byVisibleText("Nothing to show")) }
    assert(tabElements.isVisible())

    // Check tab action elements
    val tabActions = tabContents.x { byClass("CommonActionsPanel") }
    assert(tabActions.isVisible())
    val plusButton = tabActions.actionButton {
      and(byClass("ActionButton"), byAttribute("myaction", "Add (Add)"))
    }
    assert(plusButton.isVisible())
    val minusButton = tabActions.actionButton {
      and(byClass("ActionButton"), byAttribute("myaction", "Remove (Remove)"))
    }
    assert(minusButton.isVisible())
    val editButton = tabActions.actionButton {
      and(byClass("ActionButton"), byAttribute("myaction", "Edit (Edit)"))
    }
    assert(editButton.isVisible())
    if (tabText == "TSO Sessions") {
      val upButton = tabActions.actionButton {
        and(byClass("ActionButton"), byAttribute("myaction", "Up (Up)"))
      }
      assert(upButton.isVisible())
      val downButton = tabActions.actionButton {
        and(byClass("ActionButton"), byAttribute("myaction", "Down (Down)"))
      }
      assert(downButton.isVisible())
    }

    return plusButton
  }

  /**
   * Check Add Working Set dialog of any type (both for Files and JES explorer) to have correct basic functionalities
   * @param addWorkingSetDialog the dialog to check
   */
  private fun checkAddWorkingSetDialog(addWorkingSetDialog: UiComponent) {
    val wsType = if (addWorkingSetDialog.hasText("Add JES Working Set"))
      "JES Working Set"
    else
      "Files Working Set"

    // Check base fields
    val workingSetNameLabel = if (wsType == "JES Working Set") {
      addWorkingSetDialog.x { and(byClass("JLabel"), byVisibleText("JES Working Set Name: ")) }
    } else {
      addWorkingSetDialog.x { and(byClass("JLabel"), byVisibleText("Working Set Name: ")) }
    }
    assert(workingSetNameLabel.isVisible())
    val dialogInputs = addWorkingSetDialog.xx { byClass("JBTextField") }
    assert(dialogInputs.list().size == 1)
    val zosmfConnectionLabel = addWorkingSetDialog.x {
      and(byClass("JLabel"), byVisibleText("z/OSMF Connection: "))
    }
    assert(zosmfConnectionLabel.isVisible())
    val comboBoxes = addWorkingSetDialog.xx { byClass("ComboBox") }
    assert(comboBoxes.list().size == 1)

    // Check masks content
    val masksTitlePrefixToSearch = if (wsType == "JES Working Set") "Job Filters" else "DS Masks"
    val masksTitle = addWorkingSetDialog.x {
      and(byClass("JBLabel"), byVisibleText("$masksTitlePrefixToSearch included in Working Set"))
    }
    assert(masksTitle.isVisible())
    val tableHeaderTextToSearch = if (wsType == "JES Working Set") "Prefix || Owner || Job ID" else "Mask || Type"
    val tableHeader = addWorkingSetDialog.x {
      and(byClass("JBTableHeader"), byVisibleText(tableHeaderTextToSearch))
    }
    assert(tableHeader.isVisible())
    val tableElements = addWorkingSetDialog.x {
      and(byClass("ValidatingTableView"), byVisibleText("Nothing to show"))
    }
    assert(tableElements.isVisible())

    // Check dialog actions
    val dialogActions = addWorkingSetDialog.x { byClass("ActionToolbarImpl") }
    assert(dialogActions.isVisible())
    val plusButton = addWorkingSetDialog.actionButton {
      and(byClass("ActionButton"), byAttribute("myaction", "Add (Add)"))
    }
    assert(plusButton.isVisible())
    val minusButton = addWorkingSetDialog.actionButton {
      and(byClass("ActionButton"), byAttribute("myaction", "Remove (Remove)"))
    }
    assert(minusButton.isVisible())
    val upButton = addWorkingSetDialog.actionButton {
      and(byClass("ActionButton"), byAttribute("myaction", "Up (Up)"))
    }
    assert(upButton.isVisible())
    val downButton = addWorkingSetDialog.actionButton {
      and(byClass("ActionButton"), byAttribute("myaction", "Down (Down)"))
    }
    assert(downButton.isVisible())

    // Check "plus" button for Add Working Set dialog
    plusButton.setFocus()
    plusButton.click()

    tableHeader.setFocus()
    tableHeader.click()

    val newMaskEmptyRows = if (wsType == "JES Working Set") {
      addWorkingSetDialog.xx { and(byClass("ValidatingTableView"), byVisibleText("*")) }
    } else {
      addWorkingSetDialog.xx { and(byClass("ValidatingTableView"), byVisibleText("z/OS")) }
    }
    assert(newMaskEmptyRows.list().size == 1)

    // Check error tip for new empty mask row
    val newMaskEmptyRow = newMaskEmptyRows.list().first()
    newMaskEmptyRow.moveMouse(Point(1, 1))

    val tipVisibleTextToSearch = if (wsType == "JES Working Set")
      "You must provide either an owner and a prefix or a job ID."
    else
      "This field must not be blank"
    val newMaskEmptyRowTip = addWorkingSetDialog.x {
      and(byClass("Header"), byVisibleText(tipVisibleTextToSearch))
    }
    assert(newMaskEmptyRowTip.isVisible())

    // Discard the dialog
    val cancelDialogButton = addWorkingSetDialog.actionButton {
      and(byClass("JButton"), byVisibleText("Cancel"))
    }
    assert(cancelDialogButton.isVisible())
    cancelDialogButton.setFocus()
    cancelDialogButton.click()
  }

  /**
   * Check Add TSO Session dialog elements are correct
   * @param addTsoSessionDialog the dialog to check
   */
  private fun checkAddTsoSessionDialog(addTsoSessionDialog: UiComponent) {
    // Check all visible elements
    val addTsoSessionDialogChildren = addTsoSessionDialog.xx("//div[@class='DialogPanel']/div").list()
    val sessionNameLabel = addTsoSessionDialogChildren[0].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(sessionNameLabel.getText() == "Session name")
    val sessionNameInput = addTsoSessionDialogChildren[1].textField { byClass("JBTextField") }
    assert(sessionNameInput.text == "")
    val specifyConnectionLabel = addTsoSessionDialogChildren[2]
      .x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(specifyConnectionLabel.getText() == "Specify z/OSMF connection")
    assert(addTsoSessionDialogChildren[3].component.getClass().toString().contains("ComboBox"))
    val logonProcedureLabel = addTsoSessionDialogChildren[4].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(logonProcedureLabel.getText() == "Logon procedure")
    val logonProcedureInput = addTsoSessionDialogChildren[5].textField { byClass("JBTextField") }
    assert(logonProcedureInput.text == "DBSPROCC")
    val characterSetLabel = addTsoSessionDialogChildren[6].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(characterSetLabel.getText() == "Character set")
    val characterSetInput = addTsoSessionDialogChildren[7].textField { byClass("JBTextField") }
    assert(characterSetInput.text == "697")
    val codepageLabel = addTsoSessionDialogChildren[8].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(codepageLabel.getText() == "Codepage")
    assert(addTsoSessionDialogChildren[9].component.getClass().toString().contains("ComboBox"))
    assert(addTsoSessionDialogChildren[9].allTextAsString() == "1047")
    val screenRowsLabel = addTsoSessionDialogChildren[10].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(screenRowsLabel.getText() == "Screen rows")
    val screenRowsInput = addTsoSessionDialogChildren[11].textField { byClass("JBTextField") }
    assert(screenRowsInput.text == "24")
    val screenColumnsLabel = addTsoSessionDialogChildren[12].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(screenColumnsLabel.getText() == "Screen columns")
    val screenColumnsInput = addTsoSessionDialogChildren[13].textField { byClass("JBTextField") }
    assert(screenColumnsInput.text == "80")
    val accountNumberLabel = addTsoSessionDialogChildren[14].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(accountNumberLabel.getText() == "Account number")
    val accountNumberInput = addTsoSessionDialogChildren[15].textField { byClass("JBTextField") }
    assert(accountNumberInput.text == "ACCT#")
    val userGroupLabel = addTsoSessionDialogChildren[16].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(userGroupLabel.getText() == "User group")
    val userGroupInput = addTsoSessionDialogChildren[17].textField { byClass("JBTextField") }
    assert(userGroupInput.text == "GROUP1")
    val regionSizeLabel = addTsoSessionDialogChildren[18].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(regionSizeLabel.getText() == "Region size")
    val regionSizeInput = addTsoSessionDialogChildren[19].textField { byClass("JBTextField") }
    assert(regionSizeInput.text == "64000")
    val advancedParametersExpandable = addTsoSessionDialogChildren[20]
    assert(advancedParametersExpandable.component.getClass().toString().contains("CollapsibleTitledSeparatorImpl"))
    assert(advancedParametersExpandable.allTextAsString() == "Advanced Parameters")
    val resetDefaultValuesButton = addTsoSessionDialogChildren[21].button("Reset Default Values")
    assert(resetDefaultValuesButton.isVisible())

    // Check Advanced Parameters
    advancedParametersExpandable.setFocus()
    advancedParametersExpandable.click()

    val dialogScrollBar = addTsoSessionDialog.x(JScrollBarUi::class.java) { byClass("JBScrollBar") }
    assert(dialogScrollBar.isVisible())
    dialogScrollBar.scrollToMaximum()

    val childrenElementsNew = addTsoSessionDialog.xx("//div[@class='DialogPanel']/div").list()
    val reconnectTimeoutLabel = childrenElementsNew[21].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(reconnectTimeoutLabel.getText() == "Reconnect timeout (seconds)")
    val reconnectTimeoutInput = childrenElementsNew[22].textField { byClass("JBTextField") }
    assert(reconnectTimeoutInput.text == "10")
    val reconnectAttemptsLabel = childrenElementsNew[23].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(reconnectAttemptsLabel.getText() == "Reconnect max attempts")
    val reconnectAttemptsInput = childrenElementsNew[24].textField { byClass("JBTextField") }
    assert(reconnectAttemptsInput.text == "3")
    val resetDefaultValuesButtonNew = childrenElementsNew[25].button("Reset Default Values")
    assert(resetDefaultValuesButtonNew.isVisible())

    // Discard the dialog
    val cancelDialogButton = addTsoSessionDialog.actionButton {
      and(byClass("JButton"), byVisibleText("Cancel"))
    }
    assert(cancelDialogButton.isVisible())
    cancelDialogButton.setFocus()
    cancelDialogButton.click()
  }

  /**
   * Check Settings tab elements are in place
   * @param settingsDialog the settings dialog to check the tab in
   */
  private fun checkOtherSettingsTab(settingsDialog: UiComponent) {
    // Check all elements are in place
    val tabContents = settingsDialog.xx("//div[@class='DialogPanel']/div").list()
    assert(tabContents[0].allTextAsString() == "Analytics")
    val showPrivacyPolicyButton = tabContents[1].button("Show the Privacy Policy")
    assert(showPrivacyPolicyButton.isVisible())
    val privacyPolicyResult = tabContents[2].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(privacyPolicyResult.getText() == "you haven't agreed to the collection and processing of data")
    assert(tabContents[3].allTextAsString() == "Other Settings")
    val batchAmountLabel = tabContents[4].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(batchAmountLabel.getText() == "Batch amount to show per fetch")
    val batchAmountInput = tabContents[5].textField { byClass("JBTextField") }
    assert(batchAmountInput.text == "100")
    val autoSyncCheckBox = tabContents[6].checkBox { byClass("JBCheckBox") }
    assert(!autoSyncCheckBox.isSelected())
    assert(autoSyncCheckBox.text == "Enable auto-sync with mainframe")
    assert(tabContents[7].allTextAsString() == "Rate Us")
    val reviewLabel = tabContents[8].x(JLabelUiComponent::class.java) { byClass("JLabel") }
    assert(reviewLabel.getText() == "If you want to leave a review:")
    assert(tabContents[9].allTextAsString() == "click here")

    // Check Privacy Policy dialog
    showPrivacyPolicyButton.setFocus()
    showPrivacyPolicyButton.click()

    val privacyPolicyTitle = settingsDialog.x(
      "//div[@class='DialogHeader']/div[@class='JLabel' and @visible_text='For Mainframe Plugin Privacy Policy and Terms and Conditions']"
    )
    assert(privacyPolicyTitle.isVisible())
    val policyText = settingsDialog.x("//div[@class='JBTextArea' and contains(@visible_text,'Privacy Policy')]")
    assert(policyText.isVisible() && policyText.allTextAsString().contains("Information IBA Group Collects"))
    val bottomText = settingsDialog.x { byClass("DslLabel") }
    assert(bottomText.isVisible() && bottomText.allTextAsString().contains("By clicking “I agree” the User agrees"))

    val dismissButton = settingsDialog.actionButton { byText("Dismiss") }
    dismissButton.setFocus()
    dismissButton.click()
  }

  /**
   * Check all the plug-in's settings tabs and their basic functionalities
   * @param ideFrameComponent the main IDE frame component to get elements from
   * @param settingsDialog the settings dialog component to get more specific elements from
   */
  private fun checkSettings(ideFrameComponent: IdeaFrameUI, settingsDialog: UiComponent) {
    // Check that we are actually in the plug-in's settings
    val forMainframeBreadcrumb = settingsDialog.x {
      and(byClass("Breadcrumbs"), byVisibleText("For Mainframe"))
    }
    assert(forMainframeBreadcrumb.isVisible())

    val forMainframeSettingsTabs = settingsDialog.x {
      and(byClass("JBEditorTabs"), byAttribute("nextaction", "Select Next Tab (Activate next tab)"))
    }
    assert(forMainframeSettingsTabs.isVisible())

    // Check tabs
    val connectionsTab = forMainframeSettingsTabs.x {
      and(byClass("SimpleColoredComponent"), byVisibleText("Connections"))
    }
    assert(connectionsTab.isVisible())
    val jesWorkingSetsTab = forMainframeSettingsTabs.x {
      and(byClass("SimpleColoredComponent"), byVisibleText("JES Working Sets"))
    }
    assert(jesWorkingSetsTab.isVisible())
    val filesWorkingSetsTab = forMainframeSettingsTabs.x {
      and(byClass("SimpleColoredComponent"), byVisibleText("Working Sets"))
    }
    assert(filesWorkingSetsTab.isVisible())
    val tsoSessionsTab = forMainframeSettingsTabs.x {
      and(byClass("SimpleColoredComponent"), byVisibleText("TSO Sessions"))
    }
    assert(tsoSessionsTab.isVisible())
    val otherSettingsTab = forMainframeSettingsTabs.x {
      and(byClass("SimpleColoredComponent"), byVisibleText("Settings"))
    }
    assert(otherSettingsTab.isVisible())

    // Check Connections tab
    val connectionsTabPlusButton = checkSettingsTabElements(forMainframeSettingsTabs, "Connections")

    // Check "plus" action for Connections tab
    connectionsTabPlusButton.setFocus()
    connectionsTabPlusButton.click()

    // Check Add Connection dialog appears
    val addConnectionDialog = ideFrameComponent.dialog(title = "Add Connection")
    assert(addConnectionDialog.isVisible())

    val connectionDialogCancelButton = addConnectionDialog.actionButton { byVisibleText("Cancel") }
    assert(connectionDialogCancelButton.isVisible())
    connectionDialogCancelButton.setFocus()
    connectionDialogCancelButton.click()

    // Check JES Working Sets tab
    jesWorkingSetsTab.setFocus()
    jesWorkingSetsTab.click()

    val jesWorkingSetsTabPlusButton = checkSettingsTabElements(forMainframeSettingsTabs, "JES Working Sets")

    // Check "plus" action for JES Working Sets tab
    jesWorkingSetsTabPlusButton.setFocus()
    jesWorkingSetsTabPlusButton.click()

    val addJesWorkingSetDialog = settingsDialog.dialog(title = "Add JES Working Set")
    checkAddWorkingSetDialog(addJesWorkingSetDialog)

    // Check Working Sets tab
    filesWorkingSetsTab.setFocus()
    filesWorkingSetsTab.click()

    val filesWorkingSetsTabPlusButton = checkSettingsTabElements(forMainframeSettingsTabs, "Working Sets")

    // Check "plus" action for JES Working Sets tab
    filesWorkingSetsTabPlusButton.setFocus()
    filesWorkingSetsTabPlusButton.click()

    val addFilesWorkingSetDialog = settingsDialog.dialog(title = "Add Working Set")
    checkAddWorkingSetDialog(addFilesWorkingSetDialog)

    // Check TSO Sessions tab
    tsoSessionsTab.setFocus()
    tsoSessionsTab.click()

    val tsoSessionsTabPlusButton = checkSettingsTabElements(forMainframeSettingsTabs, "TSO Sessions")

    // Check "plus" action for TSO Sessions tab
    tsoSessionsTabPlusButton.setFocus()
    tsoSessionsTabPlusButton.click()

    val addTsoSessionDialog = settingsDialog.dialog(title = "Add TSO Session")
    checkAddTsoSessionDialog(addTsoSessionDialog)

    // Check Settings tab
    otherSettingsTab.setFocus()
    otherSettingsTab.click()

    checkOtherSettingsTab(settingsDialog)

    val cancelSettingsDialogButton = settingsDialog.actionButton {
      and(byClass("JButton"), byVisibleText("Cancel"))
    }
    assert(cancelSettingsDialogButton.isVisible())
    cancelSettingsDialogButton.setFocus()
    cancelSettingsDialogButton.click()
  }

  /**
   * Check the explorer view elements
   * @param ideFrameComponent the main IDE frame component to find other elements by
   * @param explorerView the explorer view to check
   * @param explorerViewType the explorer view type to check respectively
   */
  private fun checkExplorerView(
    ideFrameComponent: UiComponent,
    explorerView: UiComponent,
    explorerViewType: String
  ) {
    // Check buttons are functioning correctly for File Explorer
    val explorerViewAddButton = explorerView.actionButton { byAttribute("myicon", "add.svg") }
    assert(explorerViewAddButton.isVisible())
    explorerViewAddButton.setFocus()
    explorerViewAddButton.click()

    val plusDropdownTooltipForConnection = ideFrameComponent.x { byClass("HeavyWeightWindow") }
    val plusDropdownList = plusDropdownTooltipForConnection.list { byClass("MyList") }

    assert(plusDropdownList.rawItems.size == 3)
    assert(plusDropdownList.rawItems[0] == "Connection")
    val wsItem = if (explorerViewType == "FileExplorerView") "Working Set" else "JES Working Set"
    assert(plusDropdownList.rawItems[1] == wsItem)
    assert(plusDropdownList.rawItems[2] == "TSO Console")

    // Check disabled items are functioning correctly
    plusDropdownList.clickItemAtIndex(1) // Working Set
    assert(plusDropdownList.isVisible())
    plusDropdownList.clickItemAtIndex(2) // TSO Console
    assert(plusDropdownList.isVisible())
    plusDropdownList.hoverItemAtIndex(2) // TSO Console tooltip
    assert(
      ideFrameComponent.x { and(byClass("Header"), byVisibleText("Create connection first")) }.isVisible()
    )

    // Check 'Connection' dialog is opened and correct for File Explorer
    plusDropdownList.clickItem("Connection")
    val explorerViewConnectionDialog = ideFrameComponent.dialog(title = "Add Connection")
    assert(explorerViewConnectionDialog.isVisible())

    checkConnectionDialog(explorerViewConnectionDialog)
  }

  @Test
  @Tag("New")
  fun smokeTestCase() {
    val ideDriver = IdeRunManager.getIdeDriver()
    ideDriver.ideFrame {
      // Open the plug-in's tool window view
      val forMainframeTool = rightToolWindowToolbar.actionButton {
        byAttribute("myaction", "For Mainframe (null)")
      }
      assert(forMainframeTool.isVisible())
      forMainframeTool.setFocus()
      forMainframeTool.click()

      // Check tool window elements are in place
      val forMainframeTabs = x("//div[@class='TabPanel' and div[@class='BaseLabel' and @visible_text='For Mainframe']]")

      val fileExplorerTab = forMainframeTabs.x { byText("File Explorer") }
      assert(fileExplorerTab.isVisible())
      val jesExplorerTab = forMainframeTabs.x { byText("JES Explorer") }
      assert(jesExplorerTab.isVisible())

      // Check button elements are in place for File Explorer
      val fileExplorerView = x("//div[@class='SimpleToolWindowPanel' and div[@class='FileExplorerView']]")
      val fileExplorerViewSettingsButton = fileExplorerView.actionButton {
        byAttribute("myicon", "settings.svg")
      }
      assert(fileExplorerViewSettingsButton.isVisible())

      checkExplorerView(this, fileExplorerView, "FileExplorerView")

      // Open settings in File Explorer view
      fileExplorerViewSettingsButton.setFocus()
      fileExplorerViewSettingsButton.click()

      val settingsDialog = dialog(title = "Settings")
      assert(settingsDialog.isVisible())

      checkSettings(this, settingsDialog)

      // Check JES Explorer tab elements
      jesExplorerTab.setFocus()
      jesExplorerTab.click()

      val jesExplorerView = x("//div[@class='SimpleToolWindowPanel' and div[@class='JesExplorerView']]")
      val jesExplorerViewSettingsButton = jesExplorerView.actionButton {
        byAttribute("myicon", "settings.svg")
      }
      assert(jesExplorerViewSettingsButton.isVisible())

      checkExplorerView(this, jesExplorerView, "JesExplorerView")
    }
  }

}