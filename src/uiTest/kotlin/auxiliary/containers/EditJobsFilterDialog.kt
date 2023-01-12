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
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Edit Jobs Filter Dialog. It is a child of CreateJobsFilterDialog, since
 * it is the same dialog, just with a different name.
 */
@FixtureName("Edit Jobs Filter Dialog")
open class EditJobsFilterDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : CreateJobsFilterDialog(remoteRobot, remoteComponent) {

    companion object {
        const val name = "Edit Jobs Filter Dialog"

        /**
         * Returns the xPath of the Edit Jobs Filter Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Edit Jobs Filter' and @class='MyDialog']")
    }
}

/**
 * Finds the EditJobsFilterDialog and modifies fixtureStack.
 */
fun ContainerFixture.editJobsFilterDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: EditJobsFilterDialog.() -> Unit = {}
) {
    find<EditJobsFilterDialog>(EditJobsFilterDialog.xPath(), timeout).apply {
        fixtureStack.add(EditJobsFilterDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
