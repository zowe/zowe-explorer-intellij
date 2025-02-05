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
 *   IBA Group
 *   Uladzislau Kalesnikau
 */

package tests.utils.uidefinitions.dialogs

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.sdk.ui.components.textField
import tests.utils.AllocateDatasetParams
import tests.utils.DsOrg

val inputToIdx = mapOf(
  "Member name" to 2,

  "Primary allocation (PS-NO_MEMBER)" to 2,
  "Secondary allocation (PS-NO_MEMBER)" to 3,
  "Record len (PS-NO_MEMBER)" to 4,
  "Block size (PS-NO_MEMBER)" to 5,
  "Avg block (PS-NO_MEMBER)" to 6,

  "Primary allocation (PO-NO_MEMBER)" to 2,
  "Secondary allocation (PO-NO_MEMBER)" to 3,
  "dirBlock (PO-NO_MEMBER)" to 4,
  "Record len (PO-NO_MEMBER)" to 5,
  "Block size (PO-NO_MEMBER)" to 6,
  "Avg block (PO-NO_MEMBER)" to 7,


  "Primary allocation (PS-MEMBER)" to 3,
  "Secondary allocation (PS-MEMBER)" to 4,
  "Record len (PS-MEMBER)" to 5,
  "Block size (PS-MEMBER)" to 6,
  "Avg block (PS-MEMBER)" to 7,

  "Primary allocation (PO-MEMBER)" to 3,
  "Secondary allocation (PO-MEMBER)" to 4,
  "dirBlock (PO-MEMBER)" to 5,
  "Record len (PO-MEMBER)" to 6,
  "Block size (PO-MEMBER)" to 7,
  "Avg block (PO-MEMBER)" to 8,
)

class AllocateDatasetDialog(val driver: Driver) {

  private lateinit var allocateDsDialog: DialogUiComponent
  val okButton: ActionButtonUi by lazy { allocateDsDialog.actionButton { byVisibleText("OK") } }
  val cancelButton: ActionButtonUi by lazy { allocateDsDialog.actionButton { byVisibleText("Cancel") } }
  val datasetNameInput: JTextFieldUI by lazy { allocateDsDialog.textField("//div[@class='JBTextField']") }

  val choosePresetButton: JComboBoxUiComponent by lazy { allocateDsDialog.comboBox("(//div[@class='ComboBox'])[1]") }
  val chooseOrgButton: JComboBoxUiComponent by lazy { allocateDsDialog.comboBox("(//div[@class='ComboBox'])[2]") }
  val chooseUnitButton: JComboBoxUiComponent by lazy { allocateDsDialog.comboBox("(//div[@class='ComboBox'])[3]") }
  val chooseFormatButton: JComboBoxUiComponent by lazy { allocateDsDialog.comboBox("(//div[@class='ComboBox'])[4]") }
  val datasetParameterExpand: UiComponent by lazy { allocateDsDialog.x { byText("Dataset Parameters") } }
  val advancedParameterExpand: UiComponent by lazy { allocateDsDialog.x { byText("Advanced Parameters") } }

  init {
    driver.ideFrame {
      allocateDsDialog = dialog(title = "Allocate Dataset")
    }
  }

  private fun getJBTextFieldXPathFor(inputLabel: Int?): String {
    return "(//div[@class='JBTextField'])[${inputLabel}]"
  }

  fun fillDialog(allocationParams: AllocateDatasetParams) {
    datasetNameInput.text = allocationParams.name
    choosePresetButton.click()
    choosePresetButton.selectItem(allocationParams.preset)

    val memberName = allocationParams.memberName
    if (memberName != null) {
      driver.ideFrame {
        textField(getJBTextFieldXPathFor(inputToIdx["Member name"])).text = memberName
      }
    }

    val memberFlag = if (allocationParams.memberName != null) "MEMBER" else "NO_MEMBER"
    // To remove "-E" from "PO-E"
    val dsOrgShortGen = allocationParams.dsOrg.value.short.substringBefore("-")
    val allocationTypeKey = "$dsOrgShortGen-$memberFlag"

    datasetParameterExpand.click()
    chooseOrgButton.click()
    chooseOrgButton.selectItem(allocationParams.dsOrg.value.full)
    chooseUnitButton.click()
    chooseUnitButton.selectItem(allocationParams.unit.toString())
    chooseFormatButton.click()
    chooseFormatButton.selectItem(allocationParams.recfm.toString())

    driver.ideFrame {
      textField(getJBTextFieldXPathFor(inputToIdx["Primary allocation (${allocationTypeKey})"])).text =
        allocationParams.primAlloc ?: ""
      textField(getJBTextFieldXPathFor(inputToIdx["Secondary allocation (${allocationTypeKey})"])).text =
        allocationParams.secAlloc ?: ""
      if (allocationParams.dsOrg != DsOrg.PS) {
        textField(getJBTextFieldXPathFor(inputToIdx["dirBlock (${allocationTypeKey})"])).text =
          allocationParams.dirBlock ?: ""
      }
      textField(getJBTextFieldXPathFor(inputToIdx["Record len (${allocationTypeKey})"])).text =
        allocationParams.lrecl ?: ""
      textField(getJBTextFieldXPathFor(inputToIdx["Block size (${allocationTypeKey})"])).text =
        allocationParams.blksz ?: ""
      textField(getJBTextFieldXPathFor(inputToIdx["Avg block (${allocationTypeKey})"])).text =
        allocationParams.avgBlkLen ?: ""
    }

  }

}
