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

package org.zowe.explorer.config.connect

import org.zowe.explorer.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.zowe.kotlinsdk.annotations.ZVersion

class CredentialServiceTestSpec : WithApplicationShouldSpec({
  context("config/connect/CredentialService") {
    context("getOwner") {
      should("get owner by connection config when owner is not empty") {
        val owner = CredentialService.getOwner(
          ConnectionConfig("", "", "", true, ZVersion.ZOS_2_3, "", "ZOSMFAD")
        )

        assertSoftly { owner shouldBe "ZOSMFAD" }
      }
      should("get owner by connection config when owner is empty") {
        val owner = CredentialService.getOwner(
          ConnectionConfig("", "", "", true, ZVersion.ZOS_2_3, "", "")
        )

        assertSoftly { owner shouldBe "" }
      }
      should("get username if config owner is error string") {
        val possibleOwner = CredentialService.getOwner(
          ConnectionConfig("", "", "", true, ZVersion.ZOS_2_3, "", "COMMAND RESTARTED DUE TO ERROR")
        )
        assertSoftly { possibleOwner shouldBe "" }
      }
      should("get owner if config contains valid owner string ") {
        val possibleOwner = CredentialService.getOwner(
          ConnectionConfig("", "", "", true, ZVersion.ZOS_2_3, "", "ZOSMFAD")
        )
        assertSoftly { possibleOwner shouldBe "ZOSMFAD" }
      }
    }
  }
})