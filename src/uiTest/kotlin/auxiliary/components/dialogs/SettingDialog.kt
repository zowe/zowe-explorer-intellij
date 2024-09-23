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

import auxiliary.closable.ClosableFixtureCollector
import auxiliary.containers.ideFrameImpl
import auxiliary.containers.settingsDialog
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.utils.keyboard
import workingset.*
import workingset.auxiliary.components.elements.ButtonElement
import workingset.auxiliary.components.elements.TabElement
import java.awt.event.KeyEvent

class SettingsDialogUtil(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override var dialogTitle: String = SETTING_DIALOG
    var workingSetsTab: TabElement
    var jesWorkingSetTab: TabElement
    var connectionTab: TabElement
    private var addWsButton: ButtonElement
    var editWsButton: ButtonElement
    var okButton: ButtonElement
    var cancelButton: ButtonElement
    var removeButton: ButtonElement

    init {
        workingSetsTab = TabElement(WORKING_SETS, fixtureStack, remoteRobot)
        jesWorkingSetTab = TabElement(JES_WORKING_SETS, fixtureStack, remoteRobot)
        connectionTab = TabElement(CONNECTIONS,fixtureStack,remoteRobot)
        addWsButton = ButtonElement(addWsLoc, fixtureStack, remoteRobot)
        editWsButton = ButtonElement(editWsLoc, fixtureStack, remoteRobot)
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
        removeButton = ButtonElement(removeButtonLocAnother, fixtureStack, remoteRobot)
    }

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(Constants.remoteRobotUrl))

    // Working Set tab

    fun callAddWs(fixtureStack: MutableList<Locator>,remoteRobot: RemoteRobot, closableFixtureCollector: ClosableFixtureCollector)= with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            settingsDialog(fixtureStack) {
                addWsButton.click()
                closableFixtureCollector.add(addWorkingSetDialogLoc, fixtureStack)
            }
        }

    }

    fun selectWs(wsName: String,fixtureStack: MutableList<Locator>,remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            settingsDialog(fixtureStack) {
                findText(wsName).click()
            }
        }
    }
    fun deleteAllMask(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            find<ComponentFixture>(wsLineLoc).click()
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
            }
            val removeButton = ButtonElement(removeButtonLocAnother, fixtureStack, remoteRobot)
            if (removeButton.isEnabled()) {
                removeButton.click()
            }
        }
    }

    fun callAddConnection(fixtureStack: MutableList<Locator>,remoteRobot: RemoteRobot)= with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            settingsDialog(fixtureStack) {
                connectionTab.click()
                addWsButton.click()
            }
        }

    }

}
