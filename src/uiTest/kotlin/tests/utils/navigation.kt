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
import com.intellij.driver.sdk.ui.components.actionButton
import com.intellij.driver.sdk.ui.components.ideFrame

/**
 * Open Zowe Explorer right side panel
 * @param driver the IDE driver to use to open the panel
 */
fun openZoweExplorerPanel(driver: Driver) {
  driver.ideFrame {
    val zoweExplorerToolWindowTab = rightToolWindowToolbar.actionButton {
      byAttribute("myaction", "Zowe Explorer (null)")
    }
    zoweExplorerToolWindowTab.setFocus()
    zoweExplorerToolWindowTab.click()
  }
}
