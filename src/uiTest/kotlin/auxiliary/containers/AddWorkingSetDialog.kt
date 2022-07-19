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
import auxiliary.clickActionButton
import auxiliary.clickButton
import com.intellij.openapi.ui.ComboBox
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Class representing the Add Working Set Dialog.
 */
@FixtureName("Add Working Set Dialog")
open class AddWorkingSetDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Fills in the required information for adding a new working set.
     */
    fun addWorkingSet(workingSetName: String, connectionName: String) {
        specifyWSNameAndConnection(workingSetName, connectionName)
    }

    fun addWorkingSet(workingSetName: String, connectionName: String, mask: Pair<String,String>) {
        specifyWSNameAndConnection(workingSetName, connectionName)
        addMask(mask)
    }

    fun addWorkingSet(workingSetName: String, connectionName: String, masks: ArrayList<Pair<String,String>>) {
        specifyWSNameAndConnection(workingSetName, connectionName)
        masks.forEach { mask -> addMask(mask) }
    }

    private fun addMask(mask: Pair<String,String>) {
        clickActionButton(byXpath("//div[contains(@myvisibleactions, 'Down')]//div[contains(@myaction.key, 'button.add.a')]"))
        find<JTextFieldFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Mask || Type']]//div[@class='JBTextField']")).click()
        find<JTextFieldFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Mask || Type']]//div[@class='JBTextField']")).text = mask.first
        val findType = if (mask.first.startsWith('/')) {"USS"} else {"z/OS"}
        findAllText(findType).last().click()
        findAll<ComboBoxFixture>(byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Mask || Type']]//div[@class='JComboBox']")).last().selectItem(mask.second)
        findAllText(mask.first).last().click()
    }

    private fun specifyWSNameAndConnection(workingSetName: String, connectionName: String) {
        find<JTextFieldFixture>(byXpath("//div[@class='JBTextField']")).text = workingSetName
        if (connectionName.isNullOrEmpty().not()) {
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
        const val name = "Add Working Set Dialog"

        /**
         * Returns the xPath of the Add Working Set Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Add Working Set' and @class='MyDialog']")


    }
}

/**
 * Finds the AddWorkingSetDialog and modifies fixtureStack.
 */
fun ContainerFixture.addWorkingSetDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: AddWorkingSetDialog.() -> Unit = {}) {
    find<AddWorkingSetDialog>(AddWorkingSetDialog.xPath(), timeout).apply {
        fixtureStack.add(AddWorkingSetDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}