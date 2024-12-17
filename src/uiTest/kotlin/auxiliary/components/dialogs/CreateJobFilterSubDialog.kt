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
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import workingset.*
import workingset.Constants.remoteRobotUrl
import workingset.auxiliary.components.elements.ButtonElement
import workingset.inputFieldLoc

class CreateJobFilterSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = CREATE_JOBS_FILTER_DIALOG
    var okButton: ButtonElement
    var cancelButton: ButtonElement

    init {
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)

    }

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl)){}

    /**
     * Fills in the required information for creating a new jobs filter.
     */
    fun createJobsFilter(filter: Triple<String, String, String>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            val filterTextParams = findAll<JTextFieldFixture>(inputFieldLoc)
            filterTextParams[0].text = filter.first
            filterTextParams[1].text = filter.second
            filterTextParams[2].text = filter.third
        }
    }

}
