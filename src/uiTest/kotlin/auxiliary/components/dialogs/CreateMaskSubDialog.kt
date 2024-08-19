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

import auxiliary.containers.createMaskDialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import workingset.CREATE_MASK_DIALOG
import workingset.Constants.remoteRobotUrl
import workingset.PROJECT_NAME

class CreateMaskSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {
    override val dialogTitle: String = CREATE_MASK_DIALOG
    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    fun setMask(mask: Pair<String, String>) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createMaskDialog(fixtureStack) {
                createMask(mask)
            }
        }
    }
}