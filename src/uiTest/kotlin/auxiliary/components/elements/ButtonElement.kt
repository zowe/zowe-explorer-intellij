/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package workingset.auxiliary.components.elements

import auxiliary.RemoteRobotExtension
import auxiliary.containers.ideFrameImpl
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import workingset.PROJECT_NAME
import workingset.REMOTE_ROBOT_URL
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
open class ButtonElement(val buttonText: String, val fixtureStack: MutableList<Locator>, val remoteRobot: RemoteRobot){
    constructor() : this("",  mutableListOf<Locator>(), RemoteRobot(REMOTE_ROBOT_URL))

    internal fun isEnabled(): Boolean = with(remoteRobot) {
        var status = false
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            status = button(buttonText).isEnabled()
            }
        return status
    }

    internal fun click(waitTime: Long = 60) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            val button = button(buttonText)
            waitFor(Duration.ofSeconds(waitTime)) {
                button.isEnabled()
            }
            button.click()
        }

    }
}