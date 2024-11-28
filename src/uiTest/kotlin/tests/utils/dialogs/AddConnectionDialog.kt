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

package tests.utils.dialogs

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.sdk.ui.components.textField


class AddConnectionDialog(val driver: Driver) {

  private lateinit var connectionDialog: DialogUiComponent

  val connectionDialogPanel by lazy { connectionDialog.x { byClass("DialogPanel") } }
  val connectionNameLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Connection name: ")) } }
  private val inputFields by lazy { connectionDialogPanel.xx { byClass("JBTextField") }.list() }
  val connectionNameInput by lazy { inputFields[0].textField() }
  val urlInput by lazy { inputFields[1].textField() }
  val userNameInput by lazy { inputFields[2].textField() }
  val connectionUrlLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Connection URL: ")) } }
  val connectionUsernameLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Username: ")) } }
  val passwordLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Password: ")) } }
  val passwordInput by lazy { connectionDialog.textField { byClass("JPasswordField") } }
  val questionMark by lazy { connectionDialogPanel.x { byAttribute("defaulticon", "questionMark.svg") } }
  val questionMarkTip by lazy { connectionDialog.x { byClass("HeavyWeightWindow") } }
  val questionMarkTipParagraph by lazy { questionMarkTip.x { byClass("Paragraph") } }
  val acceptSelfSignedCheckbox by lazy {
    connectionDialogPanel.x {
      and(byClass("JBCheckBox"), byText("Accept self-signed SSL certificates"))
    }
  }
  val connectionDialogCancelButton by lazy { connectionDialog.actionButton { byVisibleText("Cancel") } }
  val connectionDialogOkButton by lazy { connectionDialog.actionButton { byVisibleText("OK") } }


  init {
    driver.ideFrame {
      connectionDialog = dialog(title = "Add Connection")
    }
  }

  /**
   * Fills in the required information for adding a new connection.
   */
  fun fillDialog(
    connectionName: String,
    connectionUrl: String,
    username: String,
    password: String,
    isAcceptSelfSigned: Boolean
  ) {
    connectionNameInput.text = connectionName
    urlInput.text = connectionUrl
    userNameInput.text = username
    passwordInput.text = password
    if (isAcceptSelfSigned) {
      acceptSelfSignedCheckbox.click()
    }
  }


}
