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

package org.zowe.explorer.config.connect

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

class CredentialsTestSpec : ShouldSpec({
  context("connect/Credentials") {
    context("toString") {
      should("not expose the password") {
        val credentials = Credentials("uuid", "username", "secret".toCharArray())

        val actual = credentials.toString()

        assertSoftly {
          actual shouldBe "Credentials{connectionConfigUuid='uuid', username='username', password='***'}"
          actual shouldNotContain "secret"
        }
      }
    }
    context("hashCode") {
      should("check hashcode for uniqueness") {
        val credentials = Credentials("uuid", "username", "password".toCharArray())
        val credentials2 = Credentials("uuid", "username", "password".toCharArray())
        val hashcode = credentials.hashCode()
        val hashcode2 = credentials2.hashCode()

        assertSoftly {
          hashcode shouldNotBe hashcode2
        }
      }
    }
  }
})
