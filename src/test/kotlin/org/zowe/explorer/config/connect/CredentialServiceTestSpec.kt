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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.config.connect

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.Application
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.annotations.ZVersion

class CredentialServiceTestSpec : AppInitShouldSpec("config/connect/CredentialService", {
  context("global context") {
    val credentialServiceMock = spyk<CredentialServiceImpl>(recordPrivateCalls = true)

    mockkConstructor(Application::class)

    beforeEach {
      every { anyConstructed<Application>().getService(CredentialService::class.java) } returns credentialServiceMock
    }

    context("all functions") {
      var isNullSet = false
      var isCredentialsSet = false

      val passSafeService = mockk<PasswordSafe> {
        every {
          get(any<CredentialAttributes>())
        } answers {
          if (firstArg<CredentialAttributes>().serviceName.endsWith("000UID")) null
          else Credentials("user", "password")
        }
        every {
          set(any<CredentialAttributes>(), any())
        } answers {
          if (secondArg<Credentials?>() == null) isNullSet = true
          else isCredentialsSet = true
        }
      }
      every { credentialServiceMock["getPasswordSafeService"]() } returns passSafeService

      beforeEach {
        isNullSet = false
        isCredentialsSet = false

        every { anyConstructed<Application>().getService(PasswordSafe::class.java) } returns passSafeService
      }

      should("getUsernameByKey") {
        val username1 = credentialServiceMock.getUsernameByKey("000UID")
        val username2 = credentialServiceMock.getUsernameByKey("validUid")

        assertSoftly {
          username1 shouldBe null
          username2 shouldBe "user"
        }
      }

      should("getPasswordByKey") {
        val password1 = credentialServiceMock.getPasswordByKey("000UID")
        val password2 = String(
          credentialServiceMock.getPasswordByKey("validUid") ?: charArrayOf()
        )

        assertSoftly {
          password1 shouldBe null
          password2 shouldBe "password"
        }
      }

      should("setCredentials") {
        credentialServiceMock
          .setCredentials("validUid", "user", "password".toCharArray())

        assertSoftly {
          isNullSet shouldBe false
          isCredentialsSet shouldBe true
        }
      }

      should("clearCredentials") {
        credentialServiceMock.clearCredentials("validUid")

        assertSoftly {
          isNullSet shouldBe true
          isCredentialsSet shouldBe false
        }
      }
    }

    context("getOwner") {
      should("get owner by connection config when owner is not empty") {
        val owner = CredentialService.getOwner(
          ConnectionConfig(
            "",
            "",
            "",
            true,
            ZVersion.ZOS_2_3,
            "",
            "ZOSMFAD"
          )
        )

        assertSoftly { owner shouldBe "ZOSMFAD" }
      }

      should("get owner by connection config when owner is empty") {
        val owner = CredentialService.getOwner(
          ConnectionConfig(
            "",
            "",
            "",
            true,
            ZVersion.ZOS_2_3,
            "",
            ""
          )
        )

        assertSoftly { owner shouldBe "" }
      }

      should("get username if config owner is error string") {
        val possibleOwner = CredentialService.getOwner(
          ConnectionConfig(
            "",
            "",
            "",
            true,
            ZVersion.ZOS_2_3,
            "",
            "COMMAND RESTARTED DUE TO ERROR"
          )
        )
        assertSoftly { possibleOwner shouldBe "" }
      }

      should("get owner if config contains valid owner string ") {
        val possibleOwner = CredentialService.getOwner(
          ConnectionConfig(
            "",
            "",
            "",
            true,
            ZVersion.ZOS_2_3,
            "",
            "ZOSMFAD"
          )
        )
        assertSoftly { possibleOwner shouldBe "ZOSMFAD" }
      }
    }
  }
})
