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
package tests.utils

import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.client.Driver

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

  fun openExplorerToolWindow(point: ActionMenuPoints): UiComponent? {
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
}
