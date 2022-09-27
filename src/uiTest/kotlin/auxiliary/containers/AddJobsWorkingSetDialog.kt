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
import auxiliary.ZOS_USERID
import auxiliary.clickActionButton
import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.keyboard
import java.awt.event.KeyEvent
import java.time.Duration

/**
 * Class representing the Add Jobs Working Set Dialog.
 */
@FixtureName("Add Jobs Working Set Dialog")
open class AddJobsWorkingSetDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Fills in the jobs working set name and connection name for adding a new empty jobs working set.
     */
    fun addJobsWorkingSet(jobsWorkingSetName: String, connectionName: String) {
        specifyJWSNameAndConnection(jobsWorkingSetName, connectionName)
    }

    /**
     * Fills in the jobs working set name, connection name and filter for adding a new jobs working set.
     */
    fun addJobsWorkingSet(jobsWorkingSetName: String, connectionName: String, filter: Triple<String, String, String>) {
        specifyJWSNameAndConnection(jobsWorkingSetName, connectionName)
        addFilter(filter)
    }

    /**
     * Fills in the jobs working set name, connection name and list of filters for adding a new jobs working set.
     */
    fun addJobsWorkingSet(
        jobsWorkingSetName: String,
        connectionName: String,
        filters: List<Triple<String, String, String>>
    ) {
        specifyJWSNameAndConnection(jobsWorkingSetName, connectionName)
        filters.forEach { addFilter(it) }
    }

    /**
     * Adds the filter to jobs working set.
     */
    fun addFilter(filter: Triple<String, String, String>) {
        clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[contains(@myaction.key, 'button.add.a')]"))
        find<JTextFieldFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Prefix || Owner || Job ID']]//div[@class='JBTextField']")).text =
            filter.first
        findAllText(ZOS_USERID).last().doubleClick()
        findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).last().text = filter.second
        keyboard {
            hotKey(KeyEvent.VK_TAB)
            hotKey(KeyEvent.VK_A)
        }
        findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).last().text = filter.third
        findAllText("Prefix").last().click()
    }

    /**
     * Deletes the filter from jobs working set.
     */
    fun deleteFilter(filter: Triple<String, String, String>) {
        val textToFind = filter.third.ifEmpty { filter.first }
        findAllText(textToFind).first().click()
        clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))
    }

    /**
     * Deletes the list of filters from jobs working set.
     */
    fun deleteFilters(filters: List<Triple<String, String, String>>) {
        filters.forEach { deleteFilter(it) }
    }

    /**
     * Deletes all filters from jobs working set.
     */
    fun deleteAllFilters() {
        find<ComponentFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Prefix || Owner || Job ID']]")).click()
        keyboard {
            hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
        }
        clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']"))
    }

    /**
     * Fills in the jobs working set name and connection name.
     */
    private fun specifyJWSNameAndConnection(jobsWorkingSetName: String, connectionName: String) {
        find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = jobsWorkingSetName
        if (connectionName.isEmpty().not()) {
            find<ComboBoxFixture>(byXpath("//div[@class='ComboBox']")).selectItem(connectionName)
        }
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Add Jobs Working Set Dialog"

        /**
         * Returns the xPath of the Add Jobs Working Set Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Add Jobs Working Set' and @class='MyDialog']")
    }
}

/**
 * Finds the AddJobsWorkingSetDialog and modifies fixtureStack.
 */
fun ContainerFixture.addJobsWorkingSetDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: AddJobsWorkingSetDialog.() -> Unit = {}
) {
    find<AddJobsWorkingSetDialog>(AddJobsWorkingSetDialog.xPath(), timeout).apply {
        fixtureStack.add(AddJobsWorkingSetDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
