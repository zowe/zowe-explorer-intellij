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
package workingset.auxiliary.components.dialogs


import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import workingset.*
import workingset.auxiliary.components.elements.ButtonElement
import java.time.Duration

class EditConnectionDialogUtil(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = EDIT_CONNECTION_DIALOG
    var okButton: ButtonElement
    var cancelButton: ButtonElement


    init {
        okButton = ButtonElement(okButtonAddConnection, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(cancelButtonAddConnection, fixtureStack, remoteRobot)

    }


    override fun waitTitle(duration: Long) = with(remoteRobot){
        find<HeavyWeightWindowFixture>(editConnectionDialogLoc, Duration.ofSeconds(duration)).findText(dialogTitle)
    }

    fun setConnectionName(connectionName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot){
        setFieldValue(0, connectionName, fixtureStack, remoteRobot)
    }

    fun setConnectionUrl(connectionUrl: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot){
        setFieldValue(1, connectionUrl, fixtureStack, remoteRobot)
    }

    fun setConnectionUserName(username: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) {
        setFieldValue(2, username, fixtureStack, remoteRobot)
    }

    private fun setFieldValue(fieldNumber: Int, value: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            findAll<JTextFieldFixture>(inputFieldLoc)[fieldNumber].text = value
        }
    }

    fun setSsl(ssl: Boolean, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            if (ssl) {
                checkBox(
                    byXpath("//div[@accessiblename='Accept self-signed SSL certificates' " +
                            "and @class='JBCheckBox' and @text='Accept self-signed SSL certificates']")
                )
                    .select()
            }
        }
    }
}
