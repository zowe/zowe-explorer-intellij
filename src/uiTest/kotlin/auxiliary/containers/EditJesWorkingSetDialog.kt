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
import com.intellij.remoterobot.fixtures.ComboBoxFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Edit JES Working Set Dialog. It is a child of AddJesWorkingSetDialog, since
 * it is the same dialog, just with a different name.
 */
class EditJesWorkingSetDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : AddJesWorkingSetDialog(remoteRobot, remoteComponent) {

    /**
     * Renames the JES working set.
     */
    fun renameJesWorkingSet(newName: String) {
        find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = newName
    }

    /**
     * Changes the connection for JES working set.
     */
    fun changeConnection(newConnectionName: String) {
        if (newConnectionName.isEmpty().not()) {
            find<ComboBoxFixture>(byXpath("//div[@class='ComboBox']")).selectItem(newConnectionName)
        }
    }

    companion object {
        const val name = "Edit JES Working Set Dialog"

        /**
         * Returns the xPath of the Edit JES Working Set Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Edit JES Working Set' and @class='MyDialog']")
    }
}

/**
 * Finds the Edit JES Working Set Dialog and modifies the fixtureStack.
 */
fun ContainerFixture.editJesWorkingSetDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: EditJesWorkingSetDialog.() -> Unit = {}
) {
    find<EditJesWorkingSetDialog>(EditJesWorkingSetDialog.xPath(), timeout).apply {
        fixtureStack.add(EditJesWorkingSetDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
