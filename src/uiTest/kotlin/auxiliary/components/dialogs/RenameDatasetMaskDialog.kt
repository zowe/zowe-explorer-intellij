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
import workingset.RENAME_DATASET_MASK_DIALOG

class RenameDatasetMaskDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = RENAME_DATASET_MASK_DIALOG

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    fun renameMaskFromContextMenu(fieldText: String){
            fillFirstField(fieldText)
        }
}
