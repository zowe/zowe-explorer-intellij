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
 *   Uladzislau Kalesnikau
 */

package tests.utils

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.jediterm.core.input.KeyEvent

/**
 * Get any explorer view that is showing on a screen right now by the [driver]
 * @return the visible explorer view as a [UiComponent] or throw otherwise
 */
fun getAnyExplorerView(driver: Driver): UiComponent {
  var explorerView: UiComponent? = null
  driver.ideFrame {
    explorerView = x("//div[@class='SimpleToolWindowPanel' and (div[@class='FileExplorerView'] or div[@class='JesExplorerView'])]")
      .waitFound()
  }
  return explorerView ?: throw Exception("No explorer view is opened")
}

/**
 * Open Zowe Explorer right side panel if it is not opened yet
 * @param driver the IDE driver to use to open the panel
 */
fun openZoweExplorerPanel(driver: Driver) {
  driver.ideFrame {
    while (isDialogOpened()) {
      keyboard { escape() }
    }
    val zoweExplorerToolWindowTab = rightToolWindowToolbar.actionButton {
      byAttribute("myaction", "Zowe Explorer (null)")
    }
    if (!zoweExplorerToolWindowTab.isSelected) {
      zoweExplorerToolWindowTab.setFocus()
      zoweExplorerToolWindowTab.click()
    }
  }
}

/**
 * Go to settings from any explorer view available by the [driver].
 * Will open the provided [tabName] or "Connections" as a default if is not provided
 * @return settings tab as a [DialogUiComponent]
 */
fun goToSettings(driver: Driver, tabName: String = ""): DialogUiComponent {
  openZoweExplorerPanel(driver)
  val explorerView = getAnyExplorerView(driver)
  var settingsDialog: DialogUiComponent? = null
  driver.ideFrame {
    val explorerViewSettingsButton = explorerView.actionButton {
      byAttribute("myicon", "settings.svg")
    }
    explorerViewSettingsButton.setFocus()
    explorerViewSettingsButton.click()

    settingsDialog = dialog(title = "Settings")
    settingsDialog?.isVisible()

    if (tabName != "") {
      val zoweExplorerSettingsTabs = settingsDialog?.x {
        and(byClass("JBEditorTabs"), byAttribute("nextaction", "Select Next Tab (Activate next tab)"))
      } ?: throw Exception("Settings dialog is not initialized")
      zoweExplorerSettingsTabs.isVisible()

      val theTab = zoweExplorerSettingsTabs.x {
        and(byClass("SimpleColoredComponent"), byVisibleText(tabName))
      }
      theTab.isVisible()
      theTab.setFocus()
      theTab.click()
    }
  }
  return settingsDialog ?: throw Exception("Settings dialog is not initialized")
}

/**
 * Delete config entities by the specified [driver].
 * Will enter the settings, select the specified [entityNameInSettings], and delete the specified [entityRowInSettings].
 * If the [entityRowInSettings] is -1, will remove all the entities on the tab
 */
fun deleteConfigEntities(driver: Driver, entityNameInSettings: String, entityRowInSettings: Int = -1) {
  val settingsDialog = goToSettings(driver, entityNameInSettings)
  driver.ideFrame {
    val zoweExplorerSettingsTabs = settingsDialog.x {
      and(byClass("JBEditorTabs"), byAttribute("nextaction", "Select Next Tab (Activate next tab)"))
    }

    var tabElements = zoweExplorerSettingsTabs.table { byClass("ValidatingTableView") }
    tabElements.isVisible()
    val didTabHasElements = tabElements.rowCount() != 0

    if (entityRowInSettings == -1) {
      while (tabElements.rowCount() != 0) {
        tabElements.clickCell(0, 0)
        keyboard { key(KeyEvent.VK_DELETE) }
        tabElements = zoweExplorerSettingsTabs.table { byClass("ValidatingTableView") }
      }
    } else {
      tabElements.clickCell(entityRowInSettings, 0)
      keyboard { key(KeyEvent.VK_DELETE) }
    }

    if (didTabHasElements) {
      // For the Apply button to change the state
      Thread.sleep(1000)

      // TODO: warning dialog handling for a connection removal

      val applySettingsDialogButton = settingsDialog.actionButton {
        and(byClass("JButton"), byVisibleText("Apply"))
      }
      applySettingsDialogButton.isVisible()
      applySettingsDialogButton.setFocus()
      applySettingsDialogButton.click()
    }

    val okSettingsDialogButton = settingsDialog.actionButton {
      and(byClass("JButton"), byVisibleText("OK"))
    }
    okSettingsDialogButton.isVisible()
    okSettingsDialogButton.setFocus()
    okSettingsDialogButton.click()
  }
}
