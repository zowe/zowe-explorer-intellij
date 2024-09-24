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

package org.zowe.explorer.config.connect

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.WithApplicationShouldSpec

class CredentialServiceTest : WithApplicationShouldSpec({

  val credentialServiceMock = spyk<CredentialServiceImpl>(recordPrivateCalls = true)
  var isNullSet = false
  var isCredentionalsSet = false

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  beforeEach {
    isNullSet = false
    isCredentionalsSet = false
  }

  context("CredentialServiceTest") {
    val passSafeService = mockk<PasswordSafe>()
    every { credentialServiceMock["getPasswordSafeService"]() } returns passSafeService
    every { passSafeService.get(any<CredentialAttributes>()) } answers {
      if (firstArg<CredentialAttributes>().serviceName.endsWith("000UID")) null
      else Credentials("user", "password")
    }
    every { passSafeService.set(any<CredentialAttributes>(), any()) } answers {
      if (secondArg<Credentials?>() == null) isNullSet = true
      else isCredentionalsSet = true
    }

    should("getUsernameByKey") {
      credentialServiceMock.getUsernameByKey("000UID") shouldBe null
      credentialServiceMock.getUsernameByKey("validUid") shouldBe "user"
    }

    should("getPasswordByKey") {
      credentialServiceMock.getPasswordByKey("000UID") shouldBe null
      credentialServiceMock.getPasswordByKey("validUid") shouldBe "password"
    }

    should("setCredentials") {
      credentialServiceMock.setCredentials("validUid", "user", "password")
      isNullSet shouldBe false
      isCredentionalsSet shouldBe true
    }

    should("clearCredentials") {
      credentialServiceMock.clearCredentials("validUid")
      isNullSet shouldBe true
      isCredentionalsSet shouldBe false
    }
  }
})