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

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import workingset.DELETION_OF_USS_PATH_ROOT
import workingset.REMOTE_ROBOT_URL

class DeletionOfUssPathRoot (fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override var dialogTitle: String = DELETION_OF_USS_PATH_ROOT

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(REMOTE_ROBOT_URL))

}