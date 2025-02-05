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
import com.intellij.driver.sdk.waitFor
import kotlin.time.Duration.Companion.seconds

// TODO: doc
class TestingConnectionDialog(val driver: Driver) {
  private lateinit var testingDialog: DialogUiComponent

  val cancelButton by lazy { testingDialog.actionButton { byVisibleText("Cancel") } }

  init {
    driver.ideFrame {
      testingDialog = dialog(locator = { and(byClass("MyDialog"), contains(".='Testing Connection to '")) })
    }
  }

  // TODO: does not work as expected...
  fun waitDialogClosed() {
    waitFor(message = "Dialog is closed", timeout = 10.seconds) {
      !testingDialog.isDialogOpened(testingDialog.searchContext.context)
    }
  }
}
