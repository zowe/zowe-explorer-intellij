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
import auxiliary.components.contentTabLabel
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Explorer.
 */
@FixtureName("Explorer")
class Explorer(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
    CommonContainerFixture(remoteRobot, remoteComponent) {

    val fileExplorer = contentTabLabel(remoteRobot, "File Explorer")
    val jesExplorer = contentTabLabel(remoteRobot, "JES Explorer")

    /**
     * Clicks on the settings action and adds the Settings Dialog to the list of fixtures needed to close.
     */
    fun settings(closableFixtureCollector: ClosableFixtureCollector, fixtureStack: List<Locator>) {
        clickActionButton(byXpath("//div[@class='ActionButton' and @myicon='settings.svg' and @myaction=' ()']"))
        closableFixtureCollector.add(SettingsDialog.xPath(), fixtureStack)
    }

    /**
     * Clicks on the add action button.
     */
    fun createConfigItem() {
        clickActionButton(byXpath("//div[@class='ActionButton' and @myicon='add.svg' and @myaction=' ()']"))
    }

    companion object {
        /**
         * Returns the xPath of the Explorer.
         */
        @JvmStatic
        fun xPath() = byXpath("//div[@class='InternalDecoratorImpl']")
    }
}

/**
 * Finds the Explorer and modifies the fixtureStack.
 */
fun ContainerFixture.explorer(function: Explorer.() -> Unit) {
    find<Explorer>(Explorer.xPath(), Duration.ofSeconds(60)).apply(function)
}
