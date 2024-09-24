/*
 * Copyright (c) 2020-2024 IBA Group.
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

package auxiliary.containers

import auxiliary.closable.ClosableCommonContainerFixture
import auxiliary.clickActionButton
import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import workingset.removeButtonLoc
import java.awt.event.KeyEvent
import java.time.Duration

/**
 * Class representing the Add Working Set Dialog.
 */
@FixtureName("Add Working Set Dialog")
open class AddWorkingSetDialog(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

  /**
   * Fills in the working set name and connection name for adding a new empty working set.
   */
  fun addWorkingSet(workingSetName: String, connectionName: String) {
    specifyWSNameAndConnection(workingSetName, connectionName)
  }

  /**
   * Fills in the working set name, connection name and mask for adding a new working set.
   */
  fun addWorkingSet(workingSetName: String, connectionName: String, mask: Pair<String, String>) {
    specifyWSNameAndConnection(workingSetName, connectionName)
    addMask(mask)
  }

  /**
   * Fills in the working set name, connection name and list of masks for adding a new working set.
   */
  fun addWorkingSet(workingSetName: String, connectionName: String, masks: List<Pair<String, String>>) {
    specifyWSNameAndConnection(workingSetName, connectionName)
    masks.forEach { addMask(it) }
  }

  /**
   * Adds the mask to working set.
   */
  fun addMask(mask: Pair<String, String>) {
    clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[contains(@myaction.key, 'button.add.a')]"))
    find<JTextFieldFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Mask || Type']]//div[@class='JBTextField']")).click()
    find<JTextFieldFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Mask || Type']]//div[@class='JBTextField']")).text =
      mask.first
    val findType = if (mask.first.startsWith('/')) "USS" else "z/OS"
    findAllText(findType).last().click()
    findAll<ComboBoxFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Mask || Type']]//div[@class='ComboBox']")).last()
      .selectItem(mask.second)
    val maskToFind = if (findType == "z/OS") mask.first.uppercase() else mask.first
    if (maskToFind.length > 48) {
      findAllText("${maskToFind.substring(0, 46)}...").last().moveMouse()
    } else {
      findAllText(maskToFind).last().moveMouse()
    }
  }

  /**
   * Deletes the mask from working set.
   */
  fun deleteMask(maskName: String) {
    if (maskName.length > 48) {
      findText("${maskName.substring(0, 46)}...").moveMouse()
    }
    findText(maskName).click()
    clickActionButton(removeButtonLoc)
  }

  /**
   * Deletes the list of masks from working set.
   */
  fun deleteMasks(masksNames: List<String>) {
    masksNames.forEach { deleteMask(it) }
  }

  /**
   * Deletes all masks from working set.
   */
  fun deleteAllMasks() {
    find<ComponentFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Mask || Type']]")).click()
    keyboard {
      hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
    }
    clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))
  }

  /**
   * Fills in the working set name and connection name.
   */
  private fun specifyWSNameAndConnection(workingSetName: String, connectionName: String) {
    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = workingSetName
    if (connectionName.isEmpty().not()) {
      find<ComboBoxFixture>(byXpath("//div[@class='ComboBox']")).selectItem(connectionName)
    }
  }

  /**
   * The close function, which is used to close the dialog in the tear down method.
   */
  override fun close() {
    clickButton("Cancel")
  }

  companion object {
    const val name = "Add Working Set Dialog"

    /**
     * Returns the xPath of the Add Working Set Dialog.
     */
    @JvmStatic
    fun xPath() = byXpath(name, "//div[@accessiblename='Add Working Set' and @class='MyDialog']")
  }
}

/**
 * Finds the AddWorkingSetDialog and modifies fixtureStack.
 */
fun ContainerFixture.addWorkingSetDialog(
  fixtureStack: MutableList<Locator>,
  timeout: Duration = Duration.ofSeconds(60),
  function: AddWorkingSetDialog.() -> Unit = {}
) {
  find<AddWorkingSetDialog>(AddWorkingSetDialog.xPath(), timeout).apply {
    fixtureStack.add(AddWorkingSetDialog.xPath())
    function()
    fixtureStack.removeLast()
  }
}
