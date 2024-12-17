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


import auxiliary.clickActionButton
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComboBoxFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.utils.keyboard
import workingset.*
import workingset.Constants.remoteRobotUrl
import workingset.auxiliary.components.elements.ButtonElement
import workingset.*
import java.awt.event.KeyEvent
import java.time.Duration

class EditJesWorkingSetSubDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot) {




    override val dialogTitle: String = ADD_JES_WORKING_SET_DIALOG
    var okButton: ButtonElement
    var cancelButton: ButtonElement
    private var actionButton: ButtonElement
    var removeButton: ButtonElement

    init {
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
        cancelButton = ButtonElement(CANCEL_TEXT, fixtureStack, remoteRobot)
        actionButton = ButtonElement(addJobFilterLoc, fixtureStack, remoteRobot)
        removeButton = ButtonElement(removeButtonLoc, fixtureStack, remoteRobot)
    }

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl)){}

    override fun waitTitle(duration: Long) = with(remoteRobot){
        find<HeavyWeightWindowFixture>(editWorkingSetDialogLocAlt, Duration.ofSeconds(duration)).findText(dialogTitle)
    }

    /**
     * Fills in the working set name.
     */
    fun setJobFilter(filter: Triple<String, String, String>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        actionButton.click()
        find<JTextFieldFixture>(jobFiltersLoc).text = filter.first
        find<JTextFieldFixture>(editWorkingSetDialogLocAlt).findAllText(ZOSMF_WORD).last().doubleClick()
        findAll<JTextFieldFixture>(inputFieldLoc).last().text = filter.second
        keyboard {
            hotKey(KeyEvent.VK_TAB)
            hotKey(KeyEvent.VK_A)
        }
        findAll<JTextFieldFixture>(inputFieldLoc).last().text = filter.third
        find<JTextFieldFixture>(editWorkingSetDialogLocAlt).findAllText(PREFIX_WORD).last().click()

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

    private fun deleteFilter(filter: Triple<String, String, String>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            val textToFind = filter.third.ifEmpty { filter.first }
            find<HeavyWeightWindowFixture>(editWorkingSetDialogLocAlt).findText(textToFind).click()
            clickActionButton(removeButtonLoc)
        }
    }

    /**
     * Deletes the list of filters from JES working set.
     */
    fun deleteFilters(filters: List<Triple<String, String, String>>, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) {
        filters.forEach { deleteFilter(it, fixtureStack, remoteRobot) }
    }
    fun deleteAllFilters(remoteRobot: RemoteRobot)  = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {

            findAll<ComponentFixture>(filterRowLoc).last().click()
            keyboard {
                hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
            }
            removeButton.click()
        }
    }



}
