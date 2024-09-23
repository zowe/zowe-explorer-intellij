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
import com.intellij.remoterobot.fixtures.ComboBoxFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.utils.keyboard
import workingset.*
import workingset.Constants.remoteRobotUrl
import workingset.auxiliary.components.elements.ButtonElement
import java.awt.event.KeyEvent
import java.time.Duration

class AddJesWorkingSetSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {

    override val dialogTitle: String = ADD_JES_WORKING_SET_DIALOG
    var okButton: ButtonElement
    var cancelButton: ButtonElement
    var actionButton: ButtonElement
    var removeButton: ButtonElement
    var removeButtonAlt: ButtonElement

    init {
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
        actionButton = ButtonElement(addJobFilterLoc, fixtureStack, remoteRobot)
        removeButton = ButtonElement(removeButtonLocAnother, fixtureStack, remoteRobot)
        removeButtonAlt = ButtonElement(removeButtonLoc, fixtureStack, remoteRobot)
    }

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    override fun waitTitle(duration: Long) = with(remoteRobot){
        find<HeavyWeightWindowFixture>(addWorkingSetDialogLocAlt, Duration.ofSeconds(duration)).findText(dialogTitle)
    }

    /**
     * Fills in the working set name.
     */
    fun setJobFilter(filter: Triple<String, String, String>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        actionButton.click()
        find<JTextFieldFixture>(jobFiltersLoc).text = filter.first
        findAll<JTextFieldFixture>(dialogRootPaneLoc).last().findAllText(ZOSMF_WORD).last().doubleClick()
        findAll<JTextFieldFixture>(inputFieldLoc).last().text = filter.second
        keyboard {
            hotKey(KeyEvent.VK_TAB)
            hotKey(KeyEvent.VK_A)
        }
        findAll<JTextFieldFixture>(inputFieldLoc).last().text = filter.third
        findAll<JTextFieldFixture>(dialogRootPaneLoc).last().findAllText(PREFIX_WORD).last().click()
    }

    /**
     * Fills in the connection name.
     */
    fun setConnectionName(connectionName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            if (connectionName.isEmpty().not()) {
                find<ComboBoxFixture>(dropdownsLoc).selectItem(connectionName)
            }
        }
    }

    fun setWsName(workingSetName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            find<JTextFieldFixture>(datasetNameInputLoc).text = workingSetName
        }
    }

    fun fillAddJobFilter(connectionName:String, workingSetName: String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        waitTitle(3)
        setWsName(workingSetName, fixtureStack, remoteRobot)
        setConnectionName(connectionName, fixtureStack, remoteRobot)
    }

    /*
    fill all fields in add working set dialog, mask -  String
    */
    fun fillAddJobFilter(connectionName:String, workingSetName: String, filter: Triple<String, String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        waitTitle(3)
        setWsName(workingSetName, fixtureStack, remoteRobot)
        setJobFilter(filter, remoteRobot)
        setConnectionName(connectionName, fixtureStack, remoteRobot)
    }

    /**
     * Fills in the JES working set name, connection name and list of filters for adding a new JES working set.
     */
    fun fillAddJobFilter(connectionName:String, workingSetName: String, filters: List<Triple<String, String, String>>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        waitTitle(3)
        setWsName(workingSetName, fixtureStack, remoteRobot)
        filters.forEach { setJobFilter(it, remoteRobot)}
        setConnectionName(connectionName, fixtureStack, remoteRobot)
    }
}
