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
 *   IBA Group
 *   Uladzislau Kalesnikau
 */

package tests.utils.uidefinitions

import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.client.Driver
import tests.utils.*
import tests.utils.notification.CreateConnectionErrorNotification
import tests.utils.notification.UnsecureConnectionDialog
import tests.utils.uidefinitions.dialogs.AddConnectionDialog

enum class ActionMenuPoints(val point: String) {
  CONNECTION("Connection"),
  WORKING_SET("Working Set"),
}

class FilesExplorerPanel(val driver: Driver) {

  lateinit var explorerView: UiComponent
  lateinit var fileExplorerTree: JTreeUiComponent
  lateinit var plusDropdownTooltipForConnection: UiComponent
  lateinit var wsMenu: JListUiComponent

  val explorerViewAddButton by lazy { explorerView.actionButton { byAttribute("myicon", "add.svg") } }
  val plusDropdownList by lazy { plusDropdownTooltipForConnection.list { byClass("MyList") } }


  init {
    driver.ideFrame {
      explorerView = x("//div[@class='SimpleToolWindowPanel' and div[@class='FileExplorerView']]")
      plusDropdownTooltipForConnection = x { byClass("HeavyWeightWindow") }
      fileExplorerTree = tree("//div[@class='DnDAwareTree']")
      wsMenu = list("//div[@class='MyMenu']")
    }
  }

  fun openDialogByPlusButtonInExplorer(point: ActionMenuPoints): UiComponent? {
    explorerViewAddButton.click()
    var dialog: UiComponent? = null
    if (point == ActionMenuPoints.CONNECTION || point == ActionMenuPoints.WORKING_SET) {
      plusDropdownList.clickItem(point.point)
      driver.ideFrame {
        dialog = dialog(title = "Add Connection")
      }
    } else {
      throw IllegalArgumentException("Unsupported point: $point")
    }
    return dialog
  }

  fun callSubMenuForRow(row: Int, menuPoint: String, submenuPoint: String? = null) {
    fileExplorerTree.rightClickRow(row)
//    Thread.sleep(1000)
    driver.ideFrame {
      x("//div[@class='MyMenu']").x { byText(menuPoint) }.click()
    }
    if (submenuPoint != null) {
      driver.ideFrame {
        x("//div[@class='ActionMenu']").x { byText(submenuPoint) }.click()
      }
    }
  }

  // TODO: move to some abstraction when ready
  /**
   * Create a valid connection in a Files Explorer view
   * @param ideDriver the IDE driver to work with other components
   * @param connectionName the connection name to use
   * @param scheme the HTTP scheme to use in the connection
   * @param host the host to connect to
   * @param port the port to connect to
   * @param username the username to connect to the server with
   * @param password the password to connect to the server with
   * @param isAllowSelfSignedStr if "true", the "Allow self-signed certificates" option will be marked
   */
  fun createValidConnection(
    ideDriver: Driver,
    connectionName: String,
    scheme: String = UI_TEST_HTTP_SCHEME,
    host: String = UI_TEST_HOST,
    port: String = UI_TEST_PORT,
    username: String = UI_TEST_USERNAME,
    password: String = UI_TEST_PASSWORD,
    isAllowSelfSignedStr: String = UI_TEST_ALLOW_SELF_SIGNED
  ) {
    val filesExplorerPanel = FilesExplorerPanel(ideDriver)
    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)
    val addConnectionDialog = AddConnectionDialog(ideDriver)
    val unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
    val (url, isAllowSelfSigned) = prepareConnectionInfo(scheme, host, port, isAllowSelfSignedStr)
    addConnectionDialog.fillDialog(connectionName, url, username, password, isAllowSelfSigned)
    if (isAllowSelfSigned) {
      unsecureConnectionDialog.proceedButton.click()
    }
    addConnectionDialog.connectionDialogOkButton.click()
    if (!url.contains("https") || isAllowSelfSigned) {
      unsecureConnectionDialog.proceedButton.click()
    }
    // TODO: find a more suitable way to track the connection is checked
    Thread.sleep(3000)
  }

  // TODO: move to some abstraction when ready
  /**
   * Create an invalid connection in a Files Explorer view
   * @param ideDriver the IDE driver to work with other components
   * @param connectionName the connection name to use
   * @param scheme the HTTP scheme to use in the connection
   * @param host the host to connect to
   * @param port the port to connect to
   * @param username the username to connect to the server with
   * @param password the password to connect to the server with
   * @param isAllowSelfSignedStr if "true", the "Allow self-signed certificates" option will be marked
   */
  fun createInvalidConnection(
    connectionName: String,
    ideDriver: Driver,
    scheme: String = UI_TEST_HTTP_SCHEME,
    host: String = UI_TEST_HOST,
    port: String = UI_TEST_PORT,
    username: String = UI_TEST_USERNAME,
    password: String = UI_TEST_PASSWORD,
    isAllowSelfSignedStr: String = UI_TEST_ALLOW_SELF_SIGNED
  ) {
    createValidConnection(ideDriver, connectionName, scheme, host, port, username, password, isAllowSelfSignedStr)
    CreateConnectionErrorNotification(ideDriver).yesButton.click()
  }
}
