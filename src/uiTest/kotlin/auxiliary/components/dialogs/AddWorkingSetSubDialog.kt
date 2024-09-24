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

import auxiliary.containers.addWorkingSetDialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComboBoxFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import workingset.*
import workingset.Constants.remoteRobotUrl
import workingset.auxiliary.components.elements.ButtonElement

class AddWorkingSetSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {




    override val dialogTitle: String = ADD_WORKING_SET_DIALOG
    var okButton: ButtonElement
    var cancelButton: ButtonElement

    init {
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
    }

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl)){}


    fun setMaskPair(mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            addWorkingSetDialog(fixtureStack) {
                addMask(mask)

            }
        }
    }

    /*
    fill all fields in add working set dialog, mask -  Pair<String, String>
    */
    fun fillAddWorkingSet(connectionName:String, wsName:String, mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        waitTitle()
        setWsName(wsName, fixtureStack, remoteRobot)
        setConnectionName(connectionName, fixtureStack, remoteRobot)
        setMaskPair(mask, fixtureStack, remoteRobot)
    }

    /**
     * Fills in the working set name.
     */
    fun setWsName(workingSetName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            find<JTextFieldFixture>(datasetNameInputLoc).text = workingSetName
        }
    }

    /**
     * Fills in the connection name.
     */
    fun setConnectionName(connectionName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            if (connectionName.isEmpty().not()) {
                find<ComboBoxFixture>(dropdownsLoc).selectItem(connectionName)
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
                setWsName(wsName, fixtureStack, remoteRobot)
                setConnectionName(connectionName, fixtureStack, remoteRobot)
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
