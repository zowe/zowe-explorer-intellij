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

package tests.utils.uidefinitions.dialogs

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*

class UnsecureConnectionDialog(val driver: Driver) {
  lateinit var dialog: DialogUiComponent
  val proceedButton by lazy { dialog.actionButton { byVisibleText("Proceed") } }

  init {
    driver.ideFrame {
<<<<<<<< HEAD:src/uiTest/kotlin/tests/utils/notification/UnsecureConnectionDialog.kt
      dialog = dialog(xpath = "//div[@accessiblename='Attempt to create an unsecured connection']")
========
      dialog = dialog("//div[@class='JEditorPane' and @visible_text='Attempt to create an unsecured connection']/ancestor::div[@class='MyDialog'][1]")
>>>>>>>> origin/feature/GH-282-automated-test-pipeline-run:src/uiTest/kotlin/tests/utils/uidefinitions/dialogs/UnsecureConnectionDialog.kt
    }
  }
}
