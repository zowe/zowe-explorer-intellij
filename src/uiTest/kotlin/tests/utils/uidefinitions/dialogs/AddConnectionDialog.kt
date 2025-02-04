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

package tests.utils.uidefinitions.dialogs

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.sdk.ui.components.textField

class AddConnectionDialog(val driver: Driver) {

  lateinit var dialogComponent: DialogUiComponent

  private val connectionDialogPanel by lazy { dialogComponent.x { byClass("DialogPanel") } }
  private val connectionNameLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Connection name: ")) } }
  private val inputFields by lazy { connectionDialogPanel.xx { byClass("JBTextField") }.list() }
  private val connectionNameInput by lazy { inputFields[0].textField() }
  private val urlInput by lazy { inputFields[1].textField() }
  private val userNameInput by lazy { inputFields[2].textField() }
  private val connectionUrlLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Connection URL: ")) } }
  private val connectionUsernameLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Username: ")) } }
  private val passwordLabel by lazy { connectionDialogPanel.x { and(byClass("JLabel"), byText("Password: ")) } }
  private val passwordInput by lazy { dialogComponent.textField { byClass("JBPasswordField") } }
  private val questionMark by lazy { connectionDialogPanel.x { byAttribute("defaulticon", "questionMark.svg") } }
  private val questionMarkTip by lazy { dialogComponent.x { byClass("HeavyWeightWindow") } }
  private val questionMarkTipParagraph by lazy { questionMarkTip.x { byClass("Paragraph") } }
  private val acceptSelfSignedCheckbox by lazy {
    connectionDialogPanel.x {
      and(byClass("JBCheckBox"), byText("Accept self-signed SSL certificates"))
    }
  }

  val cancelButton by lazy { dialogComponent.actionButton { byVisibleText("Cancel") } }
  val okButton by lazy { dialogComponent.actionButton { byVisibleText("OK") } }

  init {
    driver.ideFrame {
      dialogComponent = dialog(title = "Add Connection")
    }
  }

  /** Fills in the required information for adding a new connection */
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

  /** Check that all the dialog fields are present and visible on a screen */
  fun checkFieldsArePresent() {
    assert(connectionDialogPanel.isVisible())
    assert(connectionNameLabel.isVisible())
    assert(connectionUrlLabel.isVisible())
    assert(connectionUsernameLabel.isVisible())
    assert(passwordLabel.isVisible())
    assert(passwordInput.isVisible())
    assert(questionMark.isVisible())
    assert(cancelButton.isVisible())
  }

}
