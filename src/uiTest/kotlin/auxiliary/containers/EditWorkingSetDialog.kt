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

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Edit Working Set Dialog. It is a child of AddWorkingSetDialog, since
 * it is the same dialog, just with a different name.
 */
class EditWorkingSetDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : AddWorkingSetDialog(remoteRobot, remoteComponent) {
    companion object {
        const val name = "Edit Working Set Dialog"

        /**
         * Returns the xPath of the Edit Working Set Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Edit Working Set' and @class='MyDialog']")
    }
}

/**
 * Finds the Edit Working Set Dialog and modifies the fixtureStack.
 */
fun ContainerFixture.editWorkingSetDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: EditWorkingSetDialog.() -> Unit = {}) {
    find<EditWorkingSetDialog>(EditWorkingSetDialog.xPath(), timeout).apply {
        fixtureStack.add(EditWorkingSetDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
