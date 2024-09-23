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
import java.awt.event.KeyEvent
import java.time.Duration

/**
 * Class representing the Add JES Working Set Dialog.
 */
@FixtureName("Add JES Working Set Dialog")
open class AddJesWorkingSetDialog(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

  /**
   * Fills in the JES working set name and connection name for adding a new empty JES working set.
   */
  fun addJesWorkingSet(jesWorkingSetName: String, connectionName: String) {
    specifyJWSNameAndConnection(jesWorkingSetName, connectionName)
  }

  /**
   * Fills in the JES working set name, connection name and filter for adding a new JES working set.
   */
  fun addJesWorkingSet(
    jesWorkingSetName: String,
    connectionName: String,
    connectionUsername: String,
    filter: Triple<String, String, String>
  ) {
    specifyJWSNameAndConnection(jesWorkingSetName, connectionName)
    addFilter(connectionUsername, filter)
  }

  /**
   * Adds the filter to JES working set.
   */
  fun addFilter(connectionUsername: String, filter: Triple<String, String, String>) {
    clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[contains(@myaction.key, 'button.add.a')]"))
    find<JTextFieldFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Prefix || Owner || Job ID']]//div[@class='JBTextField']")).text =
      filter.first
    findAllText(connectionUsername.uppercase()).last().doubleClick()
    findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).last().text = filter.second
    keyboard {
      hotKey(KeyEvent.VK_TAB)
      hotKey(KeyEvent.VK_A)
    }
    findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).last().text = filter.third
    findAllText("Prefix").last().click()
  }

  /**
   * Fills in the JES working set name, connection name and list of filters for adding a new JES working set.
   */
  fun addJesWorkingSet(
    jesWorkingSetName: String,
    connectionName: String,
    connectionUsername: String,
    filters: List<Triple<String, String, String>>
  ) {
    specifyJWSNameAndConnection(jesWorkingSetName, connectionName)
    filters.forEach { addFilter(connectionUsername, it) }
  }

  /**
   * Deletes the filter from JES working set.
   */
  fun deleteFilter(filter: Triple<String, String, String>) {
    val textToFind = filter.third.ifEmpty { filter.first }
    findAllText(textToFind).first().click()
    clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))
  }

  /**
   * Deletes the list of filters from JES working set.
   */
  fun deleteFilters(filters: List<Triple<String, String, String>>) {
    filters.forEach { deleteFilter(it) }
  }

  /**
   * Deletes all filters from JES working set.
   */
  fun deleteAllFilters() {
    find<ComponentFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Prefix || Owner || Job ID']]")).click()
    keyboard {
      hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
    }
    clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))
  }

  /**
   * Fills in the JES working set name and connection name.
   */
  private fun specifyJWSNameAndConnection(jesWorkingSetName: String, connectionName: String) {
    find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = jesWorkingSetName
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
    const val name = "Add JES Working Set Dialog"

    /**
     * Returns the xPath of the Add JES Working Set Dialog.
     */
    @JvmStatic
    fun xPath() = byXpath(name, "//div[@accessiblename='Add JES Working Set' and @class='MyDialog']")
  }
}

/**
 * Finds the AddJesWorkingSetDialog and modifies fixtureStack.
 */
fun ContainerFixture.addJesWorkingSetDialog(
  fixtureStack: MutableList<Locator>,
  timeout: Duration = Duration.ofSeconds(60),
  function: AddJesWorkingSetDialog.() -> Unit = {}
) {
  find<AddJesWorkingSetDialog>(AddJesWorkingSetDialog.xPath(), timeout).apply {
    fixtureStack.add(AddJesWorkingSetDialog.xPath())
    function()
    fixtureStack.removeLast()
  }
}
