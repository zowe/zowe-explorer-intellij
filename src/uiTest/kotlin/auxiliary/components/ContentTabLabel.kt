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
 * This class represents the ContentTabLAbel.
 */
@FixtureName("ContentTabLabel")
class ContentTabLabel(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : ComponentFixture(remoteRobot, remoteComponent)

/**
 * Function, which looks for the ContentTabLabel.
 */
fun ContainerFixture.contentTabLabel(remoteRobot: RemoteRobot, text: String): ContentTabLabel {
    val xpath = byXpath("$text", "//div[@text='$text' and @class='ContentTabLabel']")
    waitFor {
        findAll<ContentTabLabel>(xpath).isNotEmpty()
    }
    return findAll<ContentTabLabel>(xpath).first()
}
