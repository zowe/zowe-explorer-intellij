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
import workingset.*
import workingset.auxiliary.components.elements.ButtonElement
import workingset.*
import java.time.Duration

class AddConnectionDialogUtil(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = EDIT_CONNECTION_DIALOG
    var okButton: ButtonElement
    var cancelButton: ButtonElement

    init {
        okButton = ButtonElement(okButtonAddConnection, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(cancelButtonAddConnection, fixtureStack, remoteRobot)

    }

    override fun waitTitle(duration: Long) = with(remoteRobot){
        find<HeavyWeightWindowFixture>(addConnectionDialogLoc, Duration.ofSeconds(duration)).findText(dialogTitle)
    }

    private fun setConnectionName(connectionName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot){
        setFieldValue(0, connectionName, fixtureStack, remoteRobot)
    }

    private fun setConnectionUrl(connectionUrl: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot){
        setFieldValue(1, connectionUrl, fixtureStack, remoteRobot)
    }

    private fun setConnectionUserName(username: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) {
        setFieldValue(2, username, fixtureStack, remoteRobot)
    }

    private fun setFieldValue(fieldNumber: Int, value: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            findAll<JTextFieldFixture>(inputFieldLoc)[fieldNumber].text = value
        }
    }

    private fun setConnectionPassword(password: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            find<JTextFieldFixture>(passwordInputLoc).text = password
        }
    }

    private fun setSsl(ssl: Boolean, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            if (ssl) {
                checkBox(sslCheckBox).select()
            }
        }
    }

    fun addConnection(connectionName: String, connectionUrl: String, username: String, password: String, ssl: Boolean)  = with(remoteRobot){
        setConnectionName(connectionName, fixtureStack, remoteRobot)
        setConnectionUrl(connectionUrl, fixtureStack, remoteRobot)
        setConnectionUserName(username, fixtureStack, remoteRobot)
        setConnectionPassword(password, fixtureStack, remoteRobot)
        setSsl(ssl, fixtureStack, remoteRobot)
    }
}
