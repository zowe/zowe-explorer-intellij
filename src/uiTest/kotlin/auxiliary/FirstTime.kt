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

package auxiliary

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import auxiliary.containers.*
import com.intellij.remoterobot.search.locators.Locator
import org.junit.jupiter.api.Tag
import workingset.PROJECT_NAME

/**
 * When adding UI tests to GitHub Actions pipeline, there is a need to first run dummy test, which
 * gets rid of tips and agrees to license agreement.
 */
@Tag("FirstTime")
@ExtendWith(RemoteRobotExtension::class)
class ConnectionManager {
    private var stack = mutableListOf<Locator>()

    @Test
    fun firstTime(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            open()
        }
        Thread.sleep(60000)
        ideFrameImpl(PROJECT_NAME, stack) {
            dialog("For Mainframe Plugin Privacy Policy and Terms and Conditions") {
                clickButton("I Agree")
            }
            close()
        }
    }
}
