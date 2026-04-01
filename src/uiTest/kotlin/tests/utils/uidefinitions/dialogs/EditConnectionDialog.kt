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

package tests.utils.uidefinitions.dialogs

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*

class EditConnectionDialog(val driver: Driver) {

  lateinit var dialogComponent: DialogUiComponent

  private val connectionDialogPanel by lazy { dialogComponent.x { byClass("DialogPanel") } }
  private val inputFields by lazy { connectionDialogPanel.xx { byClass("JBTextField") }.list() }
  private val connectionNameInput by lazy { inputFields[0].textField() }
  private val urlInput by lazy { inputFields[1].textField() }
  private val userNameInput by lazy { inputFields[2].textField() }
  private val passwordInput by lazy { dialogComponent.textField { byClass("JBPasswordField") } }
  val acceptSelfSignedCheckbox by lazy {
    connectionDialogPanel.checkBox {
      and(byClass("JBCheckBox"), byText("Accept self-signed SSL certificates"))
    }
  }

  val cancelButton by lazy { dialogComponent.actionButton { byVisibleText("Cancel") } }
  val okButton by lazy { dialogComponent.actionButton { byVisibleText("OK") } }

  init {
    driver.ideFrame {
      dialogComponent = dialog(title = "Edit Connection")
    }
  }

  fun setConnectionName(name: String) {
    connectionNameInput.text = name
  }

  fun setConnectionUrl(url: String) {
    urlInput.text = url
  }

  fun setUsername(username: String) {
    userNameInput.text = username
  }

  fun setPassword(password: String) {
    passwordInput.text = password
  }
}
