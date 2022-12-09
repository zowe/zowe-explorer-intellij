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

import auxiliary.ClosableCommonContainerFixture
import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Allocate Member Dialog.
 */
@FixtureName("Allocate Member Dialog")
open class AllocateMemberDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {
    val memberTextParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))

    /**
     * Fills in the required information for allocating a new member.
     */
    fun allocateMember(memberName: String) {
        memberTextParams[0].text = memberName
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }
    companion object {
        const val name = "Allocate Member Dialog"

        /**
         * Returns the xPath of the Allocate Member Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Create Member' and @class='MyDialog']")
    }
}

/**
 * Finds the AllocateMemberDialog and modifies fixtureStack.
 */
fun ContainerFixture.allocateMemberDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: AllocateMemberDialog.() -> Unit = {}) {
    find<AllocateMemberDialog>(AllocateMemberDialog.xPath(), timeout).apply {
        fixtureStack.add(AllocateMemberDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
