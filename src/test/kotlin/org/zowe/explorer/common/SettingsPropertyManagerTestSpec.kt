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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.common

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkConstructor
import org.zowe.explorer.testutils.AppInitShouldSpec
import java.util.*

class SettingsPropertyManagerTestSpec : AppInitShouldSpec("common/SettingsPropertyManager", {
  context("isDebugModeEnabled") {
    val propertyName = "debug.mode"

    mockkConstructor(Properties::class)

    should("debug mode enabled") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns "true"
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe true
      }
    }
    should("debug mode disabled") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns "false"
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe false
      }
    }
    should("debug mode property not found") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns null
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe false
      }
    }
    should("debug mode property contains a non-boolean value") {
      every { anyConstructed<Properties>().getProperty(propertyName) } returns "123"
      val debugMode = isDebugModeEnabled()

      assertSoftly {
        debugMode shouldBe false
      }
    }
  }
})
