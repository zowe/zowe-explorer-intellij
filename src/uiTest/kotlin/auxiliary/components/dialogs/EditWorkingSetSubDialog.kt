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

import auxiliary.containers.editWorkingSetDialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import workingset.EDIT_WORKING_SET
import workingset.PROJECT_NAME
import workingset.REMOTE_ROBOT_URL

class EditWorkingSetSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = EDIT_WORKING_SET

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(REMOTE_ROBOT_URL))

    fun renameWorkingSet(alreadyExistsWorkingSetName: String) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            editWorkingSetDialog(fixtureStack) {
                renameWorkingSet(alreadyExistsWorkingSetName)
            }
        }
    }
}
