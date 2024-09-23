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
import auxiliary.closable.ClosableFixtureCollector
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Error Creating Connection Dialog.
 */
@FixtureName("Error Creating Connection Dialog")
class ErrorCreatingConnectionDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {
    /**
     * The close function, which is used to close the Error Creating Connection Dialog in the tear down method.
     */
    override fun close() {
        clickButton("No")
    }

    companion object {
        const val name = "Error Creating Connection Dialog"
        /**
         * Returns the xPath of the Error Creating Connection Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Error Creating Connection' and @class='MyDialog']")
    }
}

/**
 * Finds the Error Creating Connection Dialog.
 *
 * After this error, the Add Connection Dialog is changed into Edit Connection Dialog.
 * This function also takes this fact into account and
 * modifies the ClosableFixtureColelctor and fixtureStack accordingly.
 */
fun ContainerFixture.errorCreatingConnectionDialog(
    closableFixtureCollector: ClosableFixtureCollector,
    fixtureStack: List<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: ErrorCreatingConnectionDialog.() -> Unit = {}) {
    find<ErrorCreatingConnectionDialog>(ErrorCreatingConnectionDialog.xPath(), timeout).apply {
        for (item in closableFixtureCollector.items) {
            if (item.name == AddConnectionDialog.name) {
                item.name = EditConnectionDialog.name
                item.fixtureStack[item.fixtureStack.size-1] = EditConnectionDialog.xPath()
            }
        }
        closableFixtureCollector.add(ErrorCreatingConnectionDialog.xPath(), fixtureStack)
        function()
    }
}
