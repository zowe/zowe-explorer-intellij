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
import com.intellij.driver.sdk.ui.components.table

class AddWorkingSetDialog(val driver: Driver) {

  private lateinit var dialogHeader: DialogUiComponent

  val wsNameInput: JTextFieldUI by lazy { dialogHeader.textField { byClass("JBTextField") } }

  val cancelButton: ActionButtonUi by lazy { dialogHeader.actionButton { byVisibleText("Cancel") } }
  val okButton: ActionButtonUi by lazy { dialogHeader.actionButton { byVisibleText("OK") } }
  val addButton: ActionButtonUi by lazy { dialogHeader.actionButton { byTooltip("Add") } }
  val removeButton: ActionButtonUi by lazy { dialogHeader.actionButton("//div[@myicon='remove.svg']") }
  val selectConnectionButton: JComboBoxUiComponent by lazy {
    dialogHeader.comboBox("//div[@class='ComboBox']")
  }
  val maskTable: JTableUiComponent by lazy { dialogHeader.table("//div[@class='ValidatingTableView']") }


  init {
    driver.ideFrame {
      dialogHeader = dialog(title = "Add Working Set")
    }
  }

  fun fillMaskItem(mask: Pair<String, String>) {
    addButton.click()

    val rowNumbers = maskTable.rowCount()

    maskTable.clickCell(rowNumbers - 1, 1)
    val maskCombobox = maskTable.comboBox("//div[@class='ComboBox']")
    maskCombobox.click()
    maskCombobox.selectItem(mask.second)

    maskTable.doubleClickCell(rowNumbers - 1, 0)
    val maskField = maskTable.textField("//div[@class='JBTextField']")
    maskField.text = mask.first
  }

  /**
   * fill all fields in add working set dialog
   * @param connectionName name of connection
   * @param wsName name of work space
   * @param mask as  Pair<String, String>
   */
  fun fillDialog(connectionName: String, wsName: String, mask: Pair<String, String>) {
    wsNameInput.text = wsName
    selectConnectionButton.click()
    selectConnectionButton.selectItem(connectionName)
    fillMaskItem(mask)
  }

  /**
   * fill all fields in add working set dialog with mask list
   * @param connectionName name of connection
   * @param wsName name of work space
   * @param mask as List<Pair<String, String>>
   */
  fun fillDialog(connectionName: String, wsName: String, mask: List<Pair<String, String>> = listOf()) {
    wsNameInput.text = wsName
    selectConnectionButton.click()
    selectConnectionButton.selectItem(connectionName)
    for (maskI in mask) fillMaskItem(maskI)
  }

}
