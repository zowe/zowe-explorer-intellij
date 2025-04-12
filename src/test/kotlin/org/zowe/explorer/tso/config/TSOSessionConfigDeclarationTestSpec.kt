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

package org.zowe.explorer.tso.config

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.spyk
import org.zowe.explorer.config.ConfigStateV2
import org.zowe.explorer.config.makeCrudableWithoutListeners
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import java.util.stream.Stream

class TSOSessionConfigDeclarationTestSpec : MockkAwareShouldSpec({
  context("tso/config/TSOSessionConfigDeclaration") {
    val crudableMock = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
    every { crudableMock.getAll(TSOSessionConfig::class.java) } answers {
      Stream.of(TSOSessionConfig())
    }

    val configDeclaration = TSOSessionConfigDeclaration()

    // ConfigDecider.canUpdate
    should("check if the config can be updated") {
      val decider = configDeclaration.getDecider(crudableMock)

      val sessionConfigA = TSOSessionConfig()
      val sessionConfigB = TSOSessionConfig()
      val resultA = decider.canUpdate(sessionConfigA, sessionConfigB)

      sessionConfigA.name = "name"
      val resultB = decider.canUpdate(sessionConfigA, sessionConfigB)

      sessionConfigB.name = "name"
      val resultC = decider.canUpdate(sessionConfigA, sessionConfigB)

      assertSoftly {
        resultA shouldBe true
        resultB shouldBe false
        resultC shouldBe true
      }
    }
    // getConfigurable
    should("get configurable") {
      val configurable = configDeclaration.getConfigurable()

      assertSoftly {
        configurable shouldNotBe null
      }
    }
  }
})
