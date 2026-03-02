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

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*
import tests.utils.*
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.uidefinitions.dialogs.ErrorCreatingConnectionDialog
import tests.utils.uidefinitions.dialogs.UnsecureConnectionDialog

/** JES explorer panel wrapper. Provides functionalities to work with JES Explorer view elements */
class JesExplorerPanel(val driver: Driver) {

  lateinit var explorerView: UiComponent
  lateinit var plusDropdownList: JListUiComponent

  val plusButton by lazy { explorerView.actionButton { byAttribute("myicon", "add.svg") } }

  init {
    openJesExplorerTab(driver)
    driver.ideFrame {
      explorerView = x("//div[@class='SimpleToolWindowPanel' and div[@class='JesExplorerView']]")
      plusDropdownList = popup().list { byClass("MyList") }
    }
  }

  /**
   * Open a respective dialog by the "+" button in JES explorer view
   * @param point the menu point to select
   */
  fun openDialogByPlusButtonInExplorer(point: ActionMenuPoints) {
    plusButton.click()
    if (
      point == ActionMenuPoints.CONNECTION ||
      point == ActionMenuPoints.JES_WORKING_SET
    ) {
      plusDropdownList.clickItem(point.point)
    } else {
      throw IllegalArgumentException("Unsupported point for JES Explorer: $point")
    }
  }

  /**
   * Create a valid connection in a JES Explorer view
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
    val jesExplorerPanel = JesExplorerPanel(ideDriver)
    jesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)
    val addConnectionDialog = AddConnectionDialog(ideDriver)
    val unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
    val (url, isAllowSelfSigned) = prepareConnectionInfo(scheme, host, port, isAllowSelfSignedStr)
    addConnectionDialog.fillDialog(connectionName, url, username, password, isAllowSelfSigned)
    if (isAllowSelfSigned) {
      unsecureConnectionDialog.proceedButton.click()
    }
    addConnectionDialog.okButton.click()
    if (!url.contains("https") || isAllowSelfSigned) {
      unsecureConnectionDialog.proceedButton.click()
    }
    // TODO: find a more suitable way to track the connection is checked
    Thread.sleep(3000)
  }

  /**
   * Create an invalid connection in a JES Explorer view
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
    ErrorCreatingConnectionDialog(ideDriver).yesButton.click()
  }
}
