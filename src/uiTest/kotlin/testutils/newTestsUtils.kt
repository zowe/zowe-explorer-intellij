/*
 * Copyright (c) 2024 IBA Group.
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

package testutils

import com.intellij.driver.sdk.ui.components.ideFrame
import com.intellij.driver.sdk.ui.components.isDialogOpened
import com.intellij.driver.sdk.ui.components.waitForNoOpenedDialogs
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.jediterm.core.input.KeyEvent

/**
 * Reset the running IDE test environment.
 * Is useful to reset the IDE state before the other tests run
 */
fun BackgroundRun.resetTestEnv(): BackgroundRun {
  driver.ideFrame {
    while (isDialogOpened()) {
      robot.pressAndReleaseKey(KeyEvent.VK_ESCAPE)
    }
    waitForNoOpenedDialogs()
  }
  return this
}
