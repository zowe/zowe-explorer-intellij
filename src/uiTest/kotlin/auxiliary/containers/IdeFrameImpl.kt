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
import auxiliary.components.actionMenu
import auxiliary.components.actionMenuItem
import auxiliary.components.stripeButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import workingset.PROJECT_NAME
import java.time.Duration

/**
 * Class representing the Ide Frame with a specific name.
 */
@FixtureName("IdeFrameImpl")
class IdeFrameImpl(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Clicks on the For Mainframe StripeButton and opens the Explorer.
     */
    fun forMainframe() {
        stripeButton(byXpath("For Mainframe", "//div[@accessiblename='For Mainframe' and @class='StripeButton' and @text='For Mainframe']"))
            .click()
    }
    companion object {
        /**
         * Returns the xPath of the Ide Frame with a specific name.
         */
        @JvmStatic
        fun xPath(name: String) = byXpath("$name", "//div[@accessiblename='$name - IntelliJ IDEA' and @class='IdeFrameImpl']")
    }

    /**
     * The close function, which is used to close the Ide Frame in the tear down method.
     */
    override fun close() {
        actionMenu(remoteRobot, "File").click()
        actionMenuItem(remoteRobot, "Close Project").click()
    }
}

/**
 * Finds the Ide Frame with a specific name and modifies the fixtureStack. The frame needs to be called from the
 * RemoteRobot as there is no ContainerFixture containing it.
 */
fun RemoteRobot.ideFrameImpl(name: String = PROJECT_NAME,
                             fixtureStack: MutableList<Locator>,
                             function: IdeFrameImpl.() -> Unit) {
    find<IdeFrameImpl>(IdeFrameImpl.xPath(name), Duration.ofSeconds(60)).apply {
        fixtureStack.add(IdeFrameImpl.xPath(name))
        function()
        fixtureStack.removeLast()
    }
}
