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
 * Class representing the Create Jobs Filter Dialog.
 */
@FixtureName("Create Jobs Filter Dialog")
open class CreateJobsFilterDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {
    private val filterTextParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))

    /**
     * Fills in the required information for creating a new jobs filter.
     */
    fun createJobsFilter(filter: Triple<String, String, String>) {
        filterTextParams[0].text = filter.first
        filterTextParams[1].text = filter.second
        filterTextParams[2].text = filter.third
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Create Jobs Filter Dialog"

        /**
         * Returns the xPath of the Create Jobs Filter Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Create Jobs Filter' and @class='MyDialog']")
    }
}

/**
 * Finds the CreateJobsFilterDialog and modifies fixtureStack.
 */
fun ContainerFixture.createJobsFilterDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: CreateJobsFilterDialog.() -> Unit = {}
) {
    find<CreateJobsFilterDialog>(CreateJobsFilterDialog.xPath(), timeout).apply {
        fixtureStack.add(CreateJobsFilterDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
