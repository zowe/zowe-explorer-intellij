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
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Create Member Dialog.
 */
@FixtureName("Create Member Dialog")
open class CreateMemberDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {
    val memberTextParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))

    /**
     * Fills in the required information for creating a new member.
     */
    fun createMember(memberName: String) {
        memberTextParams[0].text = memberName
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }
    companion object {
        const val name = "Create Member Dialog"

        /**
         * Returns the xPath of the Create Member Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Create Member' and @class='MyDialog']")
    }
}

/**
 * Finds the CreateMemberDialog and modifies fixtureStack.
 */
fun ContainerFixture.createMemberDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: CreateMemberDialog.() -> Unit = {}) {
    find<CreateMemberDialog>(CreateMemberDialog.xPath(), timeout).apply {
        fixtureStack.add(CreateMemberDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
