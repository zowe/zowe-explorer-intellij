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

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import workingset.Constants.remoteRobotUrl
import workingset.DELETION_OF_DS_MASK
import workingset.YES_TEXT
import workingset.auxiliary.components.elements.ButtonElement

class DeletionOfDSMask (fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override var dialogTitle: String = DELETION_OF_DS_MASK

    private var yesButton = ButtonElement()

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl)){
        yesButton = ButtonElement(YES_TEXT, fixtureStack, remoteRobot)
    }

}
