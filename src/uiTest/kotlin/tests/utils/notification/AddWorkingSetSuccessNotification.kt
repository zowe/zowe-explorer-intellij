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
package tests.utils.notification

import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.client.Driver

class AddWorkingSetSuccessNotification(val driver: Driver) {

  lateinit var skipButton: UiComponent

  init {
    driver.ideFrame {
      skipButton = x { byVisibleText("Skip") }
    }
  }

  fun isAllocNotificationVisible(name: String): Boolean {
    var isVisible = false
    driver.ideFrame {
      isVisible = x { byVisibleText("Dataset $name has been created") }.isVisible()
    }
    return isVisible
  }

}
