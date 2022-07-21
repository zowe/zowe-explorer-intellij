/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
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
import java.awt.event.KeyEvent
import java.time.Duration

/**
 * The representation of the Configurable Editor, which is the For Mainframe section in the Settings Dialog.
 */
@FixtureName("ConfigurableEditor")
class ConfigurableEditor(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {
    /**
     * The connection table
     */
    val conTab = tabLabel(remoteRobot, "z/OSMF Connections")

    /**
     * The Working Sets table
     */
    val workingSetsTab = tabLabel(remoteRobot, "Working Sets")

    /**
     * Clicks on the add action and adds the Add Connection Dialog to the list of fixtures needed to close.
     */
    fun add(closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        clickActionButton(byXpath("//div[@accessiblename='Add' and @class='ActionButton' and @myaction='Add (Add)']"))
        closableFixtureCollector.add(AddConnectionDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the add action and adds the Add Working Set Dialog to the list of fixtures needed to close.
     */
    fun addWS(closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        clickActionButton(byXpath("//div[@accessiblename='Add' and @class='ActionButton' and @myaction='Add (Add)']"))
        closableFixtureCollector.add(AddWorkingSetDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the edit action and adds the Edit Working Set Dialog to the list of fixtures needed to close.
     */
    fun editWorkingSet(workingSetName:String, closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        findText(workingSetName).click()
        clickActionButton(byXpath("//div[@accessiblename='Edit' and @class='ActionButton' and @myaction='Edit (Edit)']"))
        closableFixtureCollector.add(EditWorkingSetDialog.xPath(), fixtureStack)
    }
    fun deleteItem(itemName:String) {
        findText(itemName).click()
        clickActionButton(byXpath("//div[@accessiblename='Remove' and @class='ActionButton' and @myaction='Remove (Remove)']"))
    }

    fun deleteAllItems(items: String) {
        find<ComponentFixture>(byXpath("//div[@accessiblename='$items' and @class='JPanel']")).click()
        keyboard {
            hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
        }
        clickActionButton(byXpath("//div[@accessiblename='Remove' and @class='ActionButton' and @myaction='Remove (Remove)']"))
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
 * Calls the Configurable Editor, which is the For Mainframe section in the Settings Dialog.
 */
fun ContainerFixture.configurableEditor(function: ConfigurableEditor.() -> Unit) {
    find<ConfigurableEditor>(ConfigurableEditor.xPath(), Duration.ofSeconds(60)).apply(function)
}
