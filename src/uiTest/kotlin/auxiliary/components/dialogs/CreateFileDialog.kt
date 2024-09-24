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

import auxiliary.containers.createUssFileDialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import workingset.CREATE_DIRECTORY_UNDER
import workingset.CREATE_FILE_UNDER
import workingset.Constants.remoteRobotUrl
import workingset.PROJECT_NAME

class CreateFileDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override var dialogTitle: String = CREATE_FILE_UNDER

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    fun setValues(
        fileName: String,
        ownerPermissions: String,
        groupPermissions: String,
        allPermissions: String
        ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createUssFileDialog(fixtureStack) {
                createFile(fileName,ownerPermissions,groupPermissions,allPermissions,)
            }
        }
    }
}

class CreateDirectoryDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override var dialogTitle: String = CREATE_DIRECTORY_UNDER

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    fun setValues(
        fileName: String,
        ownerPermissions: String,
        groupPermissions: String,
        allPermissions: String
    ) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            createUssFileDialog(fixtureStack) {
                createFile(fileName,ownerPermissions,groupPermissions,allPermissions,)
            }
        }
    }
}
