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

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ComboBoxFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import workingset.dropdownsLoc
import java.time.Duration

/**
 * Class representing the Edit Working Set Dialog. It is a child of AddWorkingSetDialog, since
 * it is the same dialog, just with a different name.
 */
class EditWorkingSetDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : AddWorkingSetDialog(remoteRobot, remoteComponent) {

    /**
     * Renames the working set.
     */
    fun renameWorkingSet(newName: String) {
        find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = newName
    }

    /**
     * Changes the connection for working set.
     */
    fun changeConnection(newConnectionName: String) {
        if (newConnectionName.isEmpty().not()) {
            find<ComboBoxFixture>(dropdownsLoc).selectItem(newConnectionName)
        }
    }

    companion object {
        const val name = "Edit Working Set Dialog"

        /**
         * Returns the xPath of the Edit Working Set Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Edit Working Set' and @class='MyDialog']")
    }
}

/**
 * Finds the Edit Working Set Dialog and modifies the fixtureStack.
 */
fun ContainerFixture.editWorkingSetDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: EditWorkingSetDialog.() -> Unit = {}
) {
    find<EditWorkingSetDialog>(EditWorkingSetDialog.xPath(), timeout).apply {
        fixtureStack.add(EditWorkingSetDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
