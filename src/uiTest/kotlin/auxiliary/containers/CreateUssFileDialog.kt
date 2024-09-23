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
 * Class representing the Create Uss File Dialog.
 */
@FixtureName("Create Uss File Dialog")
open class CreateUssFileDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Fills in the parameters for creating uss file or folder.
     */
    fun createFile(
        fileName: String,
        ownerPermissions: String,
        groupPermissions: String,
        allPermissions: String
    ) {
        val fileTextParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        val fileComboBoxParams = findAll<ComboBoxFixture>(byXpath("//div[@class='ComboBox']"))

        fileTextParams[0].text = fileName
        fileComboBoxParams[0].selectItem(ownerPermissions)
        fileComboBoxParams[1].selectItem(groupPermissions)
        fileComboBoxParams[2].selectItem(allPermissions)
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Create Uss File Dialog"

        /**
         * Returns the xPath of the Create Uss File Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@class='MyDialog']")
    }
}

/**
 * Finds the CreateUssFileDialog and modifies fixtureStack.
 */
fun ContainerFixture.createUssFileDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: CreateUssFileDialog.() -> Unit = {}
) {
    find<CreateUssFileDialog>(CreateUssFileDialog.xPath(), timeout).apply {
        fixtureStack.add(CreateUssFileDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
