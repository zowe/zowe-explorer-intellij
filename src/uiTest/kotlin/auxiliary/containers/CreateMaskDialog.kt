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
 * Class representing the Create Mask Dialog.
 */
@FixtureName("Create Mask Dialog")
open class CreateMaskDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Fills in the required information for creating a new mask.
     */
    fun createMask(mask: Pair<String, String>) {
        find<ComboBoxFixture>(byXpath("//div[@class='ComboBox']")).selectItem(mask.second)
        find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = mask.first
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Create Mask Dialog"

        /**
         * Returns the xPath of the Create Mask Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Create Mask' and @class='MyDialog']")
    }
}

/**
 * Finds the CreateMaskDialog and modifies fixtureStack.
 */
fun ContainerFixture.createMaskDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: CreateMaskDialog.() -> Unit = {}
) {
    find<CreateMaskDialog>(CreateMaskDialog.xPath(), timeout).apply {
        fixtureStack.add(CreateMaskDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
