/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.config

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.zowe.explorer.config.connect.Credentials

class SandboxStateTestSpec : ShouldSpec({
  context("config/SandboxState") {
    context("toString") {
      should("not expose the credentials it holds") {
        val sandboxState = SandboxState(
          ConfigStateV2(),
          mutableListOf(Credentials("uuid", "username", "secret".toCharArray()))
        )

        val actual = sandboxState.toString()

        assertSoftly {
          actual shouldContain "credentials=<1 entries>"
          actual shouldNotContain "secret"
          actual shouldNotContain "username"
        }
      }
    }
  }
})
