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

package auxiliary.containers

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import java.time.Duration

/**
 * Class representing a dialog. Not CLOSABLE in tear-down method!!!
 */
@FixtureName("Dialog")
class Dialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {

    companion object {
        /**
         * Returns the xPath of the dialog depending on its title.
         */
        @JvmStatic
        fun byTitle(title: String) = byXpath("title $title", "//div[@title='$title' and @class='MyDialog']")
    }
}

/**
 * Finds a dialog. Not CLOSABLE in tear-down method!!!
 */
fun ContainerFixture.dialog(
    title: String,
    timeout: Duration = Duration.ofSeconds(20),
    function: Dialog.() -> Unit = {}): Dialog = step("Search for dialog with title $title") {
    find<Dialog>(Dialog.byTitle(title), timeout).apply(function)
}
