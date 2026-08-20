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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.api

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import org.zowe.kotlinsdk.annotations.ZVersion
import javax.swing.Icon

class CleartextConsentTestSpec : MockkAwareShouldSpec({
  context("api module: confirmCleartextUsage") {
    var dialogShownCount = 0
    var doNotAskOptionActual: DialogWrapper.DoNotAskOption? = null
    var dialogChoice = 1
    var isDoNotAskAgainSelected = false

    val appMock = mockk<Application>()
    every { appMock.invokeAndWait(any()) } answers {
      firstArg<Runnable>().run()
    }
    mockkStatic(ApplicationManager::class)
    every { ApplicationManager.getApplication() } returns appMock

    mockkStatic(Messages::class)

    beforeEach {
      dialogShownCount = 0
      doNotAskOptionActual = null
      dialogChoice = 1
      isDoNotAskAgainSelected = false

      every {
        Messages.showDialog(
          any<String>(),
          any<String>(),
          any<Array<String>>(),
          any<Int>(),
          any<Icon>(),
          any<DialogWrapper.DoNotAskOption>()
        )
      } answers {
        dialogShownCount += 1
        doNotAskOptionActual = lastArg()
        if (isDoNotAskAgainSelected) {
          doNotAskOptionActual?.setToBeShown(false, dialogChoice)
        }
        dialogChoice
      }
    }

    fun cleartextConnectionConfig() = ConnectionConfig(
      "test_uuid",
      "test_connection",
      "http://test.com:1234",
      false,
      ZVersion.ZOS_2_1
    )

    should("not ask the user when the cleartext usage is already allowed for the connection") {
      val connectionConfig = cleartextConnectionConfig().apply { isAllowCleartext = true }

      val actual = confirmCleartextUsage(connectionConfig)

      assertSoftly { actual shouldBe true }
      assertSoftly { dialogShownCount shouldBe 0 }
    }

    should("allow the cleartext usage once when the user proceeds without the 'don't ask again' option") {
      val connectionConfig = cleartextConnectionConfig()

      val firstActual = confirmCleartextUsage(connectionConfig)
      val secondActual = confirmCleartextUsage(connectionConfig)

      assertSoftly { firstActual shouldBe true }
      assertSoftly { secondActual shouldBe true }
      assertSoftly { dialogShownCount shouldBe 2 }
      assertSoftly { connectionConfig.isAllowCleartext shouldBe false }
    }

    should("store the cleartext usage consent when the user proceeds with the 'don't ask again' option") {
      isDoNotAskAgainSelected = true
      val connectionConfig = cleartextConnectionConfig()

      val firstActual = confirmCleartextUsage(connectionConfig)
      val secondActual = confirmCleartextUsage(connectionConfig)

      assertSoftly { firstActual shouldBe true }
      assertSoftly { secondActual shouldBe true }
      assertSoftly { dialogShownCount shouldBe 1 }
      assertSoftly { connectionConfig.isAllowCleartext shouldBe true }
    }

    should("decline the cleartext usage when the user chooses to go back to safety") {
      dialogChoice = 0
      val connectionConfig = cleartextConnectionConfig()

      val actual = confirmCleartextUsage(connectionConfig)

      assertSoftly { actual shouldBe false }
      assertSoftly { dialogShownCount shouldBe 1 }
      assertSoftly { connectionConfig.isAllowCleartext shouldBe false }
    }

    should("not store the cleartext usage consent when the user declines with the 'don't ask again' option") {
      dialogChoice = 0
      isDoNotAskAgainSelected = true
      val connectionConfig = cleartextConnectionConfig()

      val actual = confirmCleartextUsage(connectionConfig)

      assertSoftly { actual shouldBe false }
      assertSoftly { connectionConfig.isAllowCleartext shouldBe false }
    }
  }
})