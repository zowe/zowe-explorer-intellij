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

import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import workingset.PROJECT_NAME
import java.time.Duration

/**
 * Class representing the Welcome Frame.
 */
@FixtureName("Welcome Frame")
@DefaultXpath("type", "//div[@class='FlatWelcomeFrame']")
class WelcomeFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) :
  CommonContainerFixture(remoteRobot, remoteComponent) {
  val openProject
    get() = actionLink(
      byXpath(
        "Open Project",
        "//div[(@accessiblename='Open or Import' and @class='JButton') or (@class='MainButton' and @text='Open')]"
      )
    )

  /**
   * Opens project with projectName, which is located in the resources of the uiTest source set.
   */
  fun open() {
    openProject.click()
    Thread.sleep(1000)
    dialog("Open File or Project") {
      textField(byXpath("//div[@class='BorderlessTextField']")).text =
        System.getProperty("user.dir") + "/src/uiTest/resources/$PROJECT_NAME"
      Thread.sleep(1000)
      clickButton("OK")
    }
  }
}

/**
 * Finds the Welcome Frame and modifies the fixtureStack. The frame needs to be called from the
 * RemoteRobot as there is no ContainerFixture containing it.
 */
fun RemoteRobot.welcomeFrame(function: WelcomeFrame.() -> Unit) {
  find(WelcomeFrame::class.java, Duration.ofSeconds(60)).apply(function)
}
