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
import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Settings Dialog.
 */
@FixtureName("Settings Dialog")
class SettingsDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Settings Dialog"
        /**
         * Returns the xPath of the Settings Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Settings' and @class='MyDialog']")
    }
}

/**
 * Finds the Settings Dialog and modifies the fixtureStack.
 */
fun ContainerFixture.settingsDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: SettingsDialog.() -> Unit = {}) {
    find<SettingsDialog>(SettingsDialog.xPath(), timeout).apply {
        fixtureStack.add(SettingsDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
