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

class ErrorCreatingConnectionDialog(val driver: Driver) {
  lateinit var dialogComponent: DialogUiComponent
  val yesButton by lazy { dialogComponent.actionButton { byVisibleText("Yes") } }
  val noButton by lazy { dialogComponent.actionButton { byVisibleText("No") } }

  init {
    driver.ideFrame {
      dialogComponent = dialog(title = "Error Creating Connection")
    }
  }
}
