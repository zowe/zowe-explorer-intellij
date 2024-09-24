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
package workingset.auxiliary.components.elements

import auxiliary.RemoteRobotExtension
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import workingset.Constants.remoteRobotUrl
import workingset.PROJECT_NAME
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
open class ButtonElement(val buttonText: String, val fixtureStack: MutableList<Locator>, val remoteRobot: RemoteRobot, private val locator: Locator){
    constructor() : this("",  mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl), byXpath(""))
    constructor(locator: Locator,fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) : this("",  fixtureStack, remoteRobot, locator)
    constructor(buttonText: String,fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) : this(buttonText,  fixtureStack, remoteRobot, byXpath(""))

    internal fun isEnabled(): Boolean = with(remoteRobot) {
        var status = false
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            val button = if (buttonText.isEmpty()) button(locator) else button(buttonText)
            status = button.isEnabled()
            }
        return status
    }

    /* Click button by text if not empty else bo loc*/
    internal fun click(waitTime: Long = 60) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            val button = if (buttonText.isEmpty()) button(locator) else button(buttonText)
            waitFor(Duration.ofSeconds(waitTime)) {
                button.isEnabled()
            }
            button.click()
        }
    }

    internal fun moveMouse() = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            val button = if (buttonText.isEmpty()) button(locator) else button(buttonText)
            button.moveMouse()
        }
    }

}
