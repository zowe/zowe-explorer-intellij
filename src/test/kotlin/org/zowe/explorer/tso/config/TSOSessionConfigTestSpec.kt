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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.tso.config

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import org.zowe.kotlinsdk.TsoCodePage

class TSOSessionConfigTestSpec : MockkAwareShouldSpec({
  context("tso/config/TSOSessionConfig") {
    context("equals") {
      should("check whether two files are equal") {
        val tsoSessionConfigA = TSOSessionConfig()
        val tsoSessionConfigB = TSOSessionConfig()

        val resultA = tsoSessionConfigA.equals(tsoSessionConfigA)
        val resultB = tsoSessionConfigA.equals(tsoSessionConfigB)

        assertSoftly {
          resultA shouldBe true
          resultB shouldBe true
        }
      }
      should("check whether two files are not equal") {
        val tsoSessionConfigA = TSOSessionConfig()
        val tsoSessionConfigB = TSOSessionConfig()

        tsoSessionConfigB.name = "name"
        val resultA = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.name = "name"
        tsoSessionConfigB.connectionConfigUuid = "connectionConfigUuid"
        val resultB = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.connectionConfigUuid = "connectionConfigUuid"
        tsoSessionConfigB.logonProcedure = "logonProcedure"
        val resultC = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.logonProcedure = "logonProcedure"
        tsoSessionConfigB.charset = "charset"
        val resultD = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.charset = "charset"
        tsoSessionConfigB.codepage = TsoCodePage.IBM_1025
        val resultE = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.codepage = TsoCodePage.IBM_1025
        tsoSessionConfigB.rows = 24
        val resultF = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.rows = 24
        tsoSessionConfigB.columns = 80
        val resultG = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.columns = 80
        tsoSessionConfigB.accountNumber = "accountNumber"
        val resultH = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.accountNumber = "accountNumber"
        tsoSessionConfigB.userGroup = "userGroup"
        val resultI = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.userGroup = "userGroup"
        tsoSessionConfigB.regionSize = 64000
        val resultJ = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.regionSize = 64000
        tsoSessionConfigB.timeout = 10
        val resultK = tsoSessionConfigA.equals(tsoSessionConfigB)

        tsoSessionConfigA.timeout = 10L
        tsoSessionConfigB.maxAttempts = 3
        val resultL = tsoSessionConfigA.equals(tsoSessionConfigB)

        assertSoftly {
          resultA shouldBe false
          resultB shouldBe false
          resultC shouldBe false
          resultD shouldBe false
          resultE shouldBe false
          resultF shouldBe false
          resultG shouldBe false
          resultH shouldBe false
          resultI shouldBe false
          resultJ shouldBe false
          resultK shouldBe false
          resultL shouldBe false
        }
      }
    }
  }

  context("hashCode") {
    should("check hashcode for equality") {
      val tsoSessionConfigA = TSOSessionConfig()
      val tsoSessionConfigB = TSOSessionConfig()
      val hashCodeA = tsoSessionConfigA.hashCode()
      val hashCodeB = tsoSessionConfigB.hashCode()

      assertSoftly {
        hashCodeA shouldBe hashCodeB
      }
    }
  }

  context("toString") {
    should("generate string version of the TSOSessionConfig instance") {
      val tsoSessionConfig = TSOSessionConfig(
        "uuid",
        "name",
        "connectionConfigUuid",
        "logonProcedure",
        "charset",
        TsoCodePage.IBM_1025,
        24,
        80,
        "accountNumber",
        "userGroup",
        64000,
        10L,
        3
      )

      val result = tsoSessionConfig.toString()

      assertSoftly { result shouldBe "TSOSessionConfig(name='name', connectionConfigUuid='connectionConfigUuid', logonProcedure='logonProcedure', charset='charset', codepage=1025, rows=24, columns=80, accountNumber=accountNumber, userGroup=userGroup, regionSize=64000, timeout=10, maxAttempts=3)" }
    }
  }

  context("toDialogState") {
    should("convert TSO session config to dialog state") {
      val tsoSessionConfig = TSOSessionConfig(
        "uuid",
        "name",
        "connectionConfigUuid",
        "logonProcedure",
        "charset",
        TsoCodePage.IBM_1025,
        24,
        80,
        "accountNumber",
        "userGroup",
        64000,
        10L,
        3
      )

      val tsoDialogState = tsoSessionConfig.toDialogState()

      assertSoftly {
        tsoSessionConfig.uuid shouldBe tsoDialogState.uuid
        tsoSessionConfig.name shouldBe tsoDialogState.name
        tsoSessionConfig.connectionConfigUuid shouldBe tsoDialogState.connectionConfigUuid
        tsoSessionConfig.logonProcedure shouldBe tsoDialogState.logonProcedure
        tsoSessionConfig.charset shouldBe tsoDialogState.charset
        tsoSessionConfig.codepage shouldBe tsoDialogState.codepage
        tsoSessionConfig.rows shouldBe tsoDialogState.rows
        tsoSessionConfig.columns shouldBe tsoDialogState.columns
        tsoSessionConfig.accountNumber shouldBe tsoDialogState.accountNumber
        tsoSessionConfig.userGroup shouldBe tsoDialogState.userGroup
        tsoSessionConfig.regionSize shouldBe tsoDialogState.regionSize
        tsoSessionConfig.timeout shouldBe tsoDialogState.timeout
        tsoSessionConfig.maxAttempts shouldBe tsoDialogState.maxAttempts
      }
    }
  }
})
