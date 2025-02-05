/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   IBA Group
 *   Uladzislau Kalesnikau
 */

package tests.utils

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

/**
 * Prepare connection URL and "Allow self-signed certificates" options basing on the provided data.
 * If scheme, host, and port are provided as "undef", the URL will be provided as the mock server URL
 * @param scheme the HTTP scheme to form the URL
 * @param host the host to form the URL
 * @param port the port to form the URL
 * @param isAllowSelfSignedStr if "true", the "Allow self-signed certificates" option will be true, false otherwise
 * @return a pair of URL and "Allow self-signed certificates"
 */
fun prepareConnectionInfo(scheme: String, host: String, port: String, isAllowSelfSignedStr: String): Pair<String, Boolean> {
  val url = if (scheme == "undef" && host == "undef" && port == "undef") {
    MockWebServerManager.url
  } else "$scheme://$host:$port"

  val isAllowSelfSigned = if (isAllowSelfSignedStr == "undef") false else isAllowSelfSignedStr == "true"

  return url to isAllowSelfSigned
}
