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

import auxiliary.containers.editWorkingSetDialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComboBoxFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import workingset.Constants.remoteRobotUrl
import workingset.EDIT_WORKING_SET
import workingset.PROJECT_NAME
import workingset.datasetNameInputLoc
import workingset.dropdownsLoc

class EditWorkingSetSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = EDIT_WORKING_SET

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    fun renameWorkingSet(workingSetName: String) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                find<JTextFieldFixture>(datasetNameInputLoc).text = workingSetName
            }
        }
    }

    fun setMaskPair(mask: Pair<String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                addMask(mask)

            }
        }
    }

    fun deleteMasks(masks: List<String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                deleteMasks(masks)

            }
        }
    }

    fun setConnection(connectionName:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                find<ComboBoxFixture>(dropdownsLoc).selectItem(connectionName)

            }
        }
    }

}
