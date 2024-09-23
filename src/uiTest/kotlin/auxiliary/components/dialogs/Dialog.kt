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

import auxiliary.RemoteRobotExtension
import auxiliary.containers.dialog
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import workingset.*
import workingset.Constants.remoteRobotUrl
import workingset.auxiliary.components.elements.ButtonElement
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
abstract class AbstractDialog(internal var fixtureStack: MutableList<Locator>, internal var remoteRobot: RemoteRobot){

    abstract val dialogTitle: String

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    internal fun isShown(): Boolean = with(remoteRobot) {
        var status = false
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            status = dialog(dialogTitle).isShowing
        }
        return status
    }

    internal fun fillFirstField(fieldText: String) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            dialog(dialogTitle) {
                find<JTextFieldFixture>(datasetNameInputLoc).text = fieldText
            }
        }
    }

    internal fun clickButtonByName(button_name: String) = with(remoteRobot) {
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            dialog(dialogTitle) {
                ButtonElement(button_name, fixtureStack, remoteRobot).click()
            }
        }
    }


    internal open fun waitTitle(duration: Long=3) = with(remoteRobot){
        find<HeavyWeightWindowFixture>(myDialogXpathLoc, Duration.ofSeconds(duration)).findText(dialogTitle)
    }

    fun moveToText(text:String, fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            dialog(dialogTitle) {//sub dialog locators
                findText(text).moveMouse()
            }
        }
    }

}
