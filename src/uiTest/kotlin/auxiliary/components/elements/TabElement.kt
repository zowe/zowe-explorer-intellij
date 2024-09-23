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
import auxiliary.components.TabLabel
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
open class TabElement(tabText: String, val fixtureStack: MutableList<Locator>, val remoteRobot: RemoteRobot){
    constructor() : this("",  mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    private val tabXpath = byXpath(tabText, "//div[@accessiblename='$tabText' and @class='TabLabel']")

    /* Click button by text */
    internal fun click(waitTime: Long = 60) = with(remoteRobot){
        ideFrameImpl(PROJECT_NAME, fixtureStack) {
            waitFor(Duration.ofSeconds(waitTime)) {
                findAll<TabLabel>(tabXpath).isNotEmpty()
            }
            findAll<TabLabel>(tabXpath).first().click()
        }
    }
}
