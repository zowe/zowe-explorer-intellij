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

import auxiliary.containers.dialog
import auxiliary.containers.editWorkingSetDialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions
import workingset.*
import java.time.Duration

class RenameDatasetMaskDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = RENAME_DATASET_MASK_DIALOG

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(REMOTE_ROBOT_URL))
    
    fun renameMaskFromContextMenu(
        fieldText: String,
        remoteRobot: RemoteRobot
    ) =
        with(remoteRobot) {
            fillFirstFilld(fieldText)

        }
}

