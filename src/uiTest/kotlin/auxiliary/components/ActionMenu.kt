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

package auxiliary.components

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor

/**
 * This class was copied from ui-robot at jet brains
 *
 * It represents the ActionMenu
 */
@FixtureName("ActionMenu")
class ActionMenuFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : ComponentFixture(remoteRobot, remoteComponent)

/**
 * Function, which looks for the ActionMenu.
 */
fun ContainerFixture.actionMenu(remoteRobot: RemoteRobot, text: String): ActionMenuFixture {
    val xpath = byXpath("text '$text'", "//div[@class='ActionMenu' and @text='$text']")
    waitFor {
        findAll<ActionMenuFixture>(xpath).isNotEmpty()
    }
    return findAll<ActionMenuFixture>(xpath).first()
}
