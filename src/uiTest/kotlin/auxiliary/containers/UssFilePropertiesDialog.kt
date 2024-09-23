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
 * Class representing the Uss File Properties Dialog.
 */
@FixtureName("Uss File Properties Dialog")
open class UssFilePropertiesDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Checks if the provided uss file properties and the properties in opened Uss File Properties Dialog are matching.
     */
    fun areUssFilePropertiesValid(
        fileName: String,
        location: String,
        path: String,
        size: String,
        modDate: String,
        owner: String,
        group: String,
        groupId: String,
        ownerPerm: String,
        groupPerm: String,
        allPerm: String): Boolean {
        var result = true
        val fileGeneralParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(fileGeneralParams[0].text != fileName || fileGeneralParams[1].text != location ||
            fileGeneralParams[2].text != path || fileGeneralParams[3].text != size ||
            fileGeneralParams[4].text != modDate){
            result = false
        }

        find<ComponentFixture>(byXpath("//div[@text='Permissions']")).click()
        Thread.sleep(1000)
        val filePermParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(filePermParams[0].text != owner || filePermParams[1].text != group ||
            filePermParams[2].text != groupId){
            result = false
        }

        val filePermissions = findAll<ComboBoxFixture>(byXpath("//div[@class='ComboBox']"))
        if(filePermissions[0].selectedText() != ownerPerm || filePermissions[1].selectedText() != groupPerm ||
            filePermissions[2].selectedText() != allPerm){
            result = false
        }

        return result
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Uss File Properties Dialog"

        /**
         * Returns the xPath of the Uss File Properties Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@class='MyDialog']")
    }
}

/**
 * Finds the UssFilePropertiesDialog and modifies fixtureStack.
 */
fun ContainerFixture.ussFilePropertiesDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: UssFilePropertiesDialog.() -> Unit = {}
) {
    find<UssFilePropertiesDialog>(UssFilePropertiesDialog.xPath(), timeout).apply {
        fixtureStack.add(UssFilePropertiesDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
