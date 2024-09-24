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

import auxiliary.clickActionButton
import auxiliary.closable.ClosableFixtureCollector
import auxiliary.components.tabLabel
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import workingset.addWsLoc
import workingset.editWsLoc
import workingset.removeButtonFromConfigLoc
import workingset.wsLineLoc
import java.awt.event.KeyEvent
import java.time.Duration

/**
 * The representation of the Configurable Editor, which is the Zowe Explorer section in the Settings Dialog.
 */
@FixtureName("ConfigurableEditor")
class ConfigurableEditor(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
    CommonContainerFixture(remoteRobot, remoteComponent) {
    /**
     * The connection table
     */
    val conTab = tabLabel(remoteRobot, "Connections")

    /**
     * The Working Sets table
     */
    val workingSetsTab = tabLabel(remoteRobot, "Working Sets")

    /**
     * The JES Working Sets table
     */
    val jesWorkingSetsTab = tabLabel(remoteRobot, "JES Working Sets")

    /**
     * Clicks on the add action and adds the Add Connection Dialog to the list of fixtures needed to close.
     */
    fun add(closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        clickActionButton(addWsLoc)
        closableFixtureCollector.add(AddConnectionDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the add action and adds the Add Working Set Dialog to the list of fixtures needed to close.
     */
    fun addWS(closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        clickActionButton(addWsLoc)
        closableFixtureCollector.add(AddWorkingSetDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the edit action and adds the Edit Working Set Dialog to the list of fixtures needed to close.
     */
    fun editWorkingSet(
        workingSetName: String,
        closableFixtureCollector: ClosableFixtureCollector,
        fixtureStack: List<Locator>
    ) {
        findText(workingSetName).click()
        clickActionButton(editWsLoc)
        closableFixtureCollector.add(EditWorkingSetDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the add action and adds the Add JES Working Set Dialog to the list of fixtures needed to close.
     */
    fun addJWS(closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        clickActionButton(addWsLoc)
        closableFixtureCollector.add(AddJesWorkingSetDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the edit action and adds the Edit JES Working Set Dialog to the list of fixtures needed to close.
     */
    fun editJesWorkingSet(
        jesWorkingSetName: String,
        closableFixtureCollector: ClosableFixtureCollector,
        fixtureStack: List<Locator>
    ) {
        findText(jesWorkingSetName).click()
        clickActionButton(editWsLoc)
        closableFixtureCollector.add(EditJesWorkingSetDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the edit action and adds the Edit Connection Dialog to the list of fixtures needed to close.
     */
    fun editConnection(
        connectionName: String,
        closableFixtureCollector: ClosableFixtureCollector,
        fixtureStack: List<Locator>
    ) {
        findText(connectionName).click()
        clickActionButton(editWsLoc)
        closableFixtureCollector.add(EditConnectionDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the remove action and deletes the item from config table.
     */
    fun deleteItem(itemName: String) {
        findText(itemName).click()
        clickActionButton(removeButtonFromConfigLoc)
    }

    /**
     * Press Ctrl+A and clicks on the remove action to delete all items from the table.
     */
    fun deleteAllItems() {
        find<ComponentFixture>(wsLineLoc).click()
        keyboard {
            hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
        }
        if (button(removeButtonFromConfigLoc).isEnabled()) {
            clickActionButton(removeButtonFromConfigLoc)
        }
    }

    companion object {
        /**
         * Returns the xPath of the Configurable Editor.
         */
        @JvmStatic
        fun xPath() = byXpath("//div[@class='ConfigurableEditor']")
    }
}

/**
 * Calls the Configurable Editor, which is the Zowe Explorer section in the Settings Dialog.
 */
fun ContainerFixture.configurableEditor(function: ConfigurableEditor.() -> Unit) {
    find<ConfigurableEditor>(ConfigurableEditor.xPath(), Duration.ofSeconds(60)).apply(function)
}
