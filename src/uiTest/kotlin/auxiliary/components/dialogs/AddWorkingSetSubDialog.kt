/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package workingset.auxiliary.components.dialogs

import auxiliary.containers.addWorkingSetDialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import workingset.*
import workingset.auxiliary.components.elements.ButtonElement

class AddWorkingSetSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = ADD_WORKING_SET_DIALOG
    var okButton = ButtonElement()
    var cancelButton = ButtonElement()

    init {
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
    }

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(REMOTE_ROBOT_URL)){}
    /*
    fill all fields in add working set dialog, mask -  Pair<String, String>
    */
    fun fillAddWorkingSet(connectionName:String, wsName:String, mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        waitTitle()
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, mask)
            }
        }
    }

    /*
    fill all fields in add working set dialog, mask -  String
    */
    fun fillAddWorkingSet(connectionName:String, wsName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        waitTitle()
        ideFrameImpl(PROJECT_NAME, fixtureStack) {

            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName)
            }
        }
    }

    /*
    fill all fields in add working set dialog, mask -  ArrayList<Pair<String, String>>
    */
    fun fillAddWorkingSet(connectionName:String, wsName:String, mask: ArrayList<Pair<String, String>>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        waitTitle()
        ideFrameImpl(PROJECT_NAME, fixtureStack) {

            addWorkingSetDialog(fixtureStack) {
                addWorkingSet(wsName, connectionName, mask)
            }
        }
    }
}