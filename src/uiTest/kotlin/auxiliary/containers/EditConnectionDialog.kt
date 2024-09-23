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
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Edit Connection Dialog. It is a child of AddConnectionDialog, since
 * it is the same dialog, just with a different name.
 */
class EditConnectionDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : AddConnectionDialog(remoteRobot, remoteComponent) {

    /**
     * Unchecks checkBox for SSL certificates.
     */
    fun uncheckSSLBox() {
        checkBox(
            byXpath(
                "//div[@accessiblename='Accept self-signed SSL certificates' " +
                        "and @class='JBCheckBox' and @text='Accept self-signed SSL certificates']"
            )
        ).unselect()
    }

    companion object {
        const val name = "Edit Connection Dialog"

        /**
         * Returns the xPath of the Edit Connection Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Edit Connection' and @class='MyDialog']")
    }
}

/**
 * Finds the Edit Connection Dialog and modifies the fixtureStack.
 */
fun ContainerFixture.editConnectionDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: EditConnectionDialog.() -> Unit = {}
) {
    find<EditConnectionDialog>(EditConnectionDialog.xPath(), timeout).apply {
        fixtureStack.add(EditConnectionDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
