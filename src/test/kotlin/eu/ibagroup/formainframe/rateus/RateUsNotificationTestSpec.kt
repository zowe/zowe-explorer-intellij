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

package eu.ibagroup.formainframe.rateus

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestConfigServiceImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.reflect.KFunction

class RateUsNotificationTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }

  context("rateus module: RateUsNotification") {
    context("showRateUsNotification") {
      // Just to cover the class usage
      RateUsNotification()

      var isBrowseTriggered = false

      val configService = ConfigService.getService() as TestConfigServiceImpl
      val browseRef: (String) -> Unit = BrowserUtil::browse
      val notifyRef: (Notification) -> Unit = Notifications.Bus::notify

      beforeEach {
        mockkStatic(browseRef as KFunction<*>)
        every {
          browseRef(any<String>())
        } answers {
          isBrowseTriggered = true
        }

        mockkStatic(notifyRef as KFunction<*>)
        mockkStatic(Notification::get)
      }

      afterEach {
        isBrowseTriggered = false

        unmockkAll()
        configService.resetTestService()
      }

      should("the 'Rate us' notification is appeared and the 'Rate' button is clicked") {
        every { Notifications.Bus.notify(any<Notification>()) } answers {
          val notification = firstArg<Notification>()
          every { Notification.get(any()) } returns notification
          val rateAction = notification.actions.first { it.templateText == "Rate" }
          rateAction.actionPerformed(mockk())
        }
        RateUsNotification.showRateUsNotification()
        assertSoftly { isBrowseTriggered shouldBe true }
        assertSoftly { configService.isRateUsNotificationDelayChanged shouldBe true }
      }
      should("the 'Rate us' notification is appeared and the 'Later' button is clicked") {
        every { Notifications.Bus.notify(any<Notification>()) } answers {
          val notification = firstArg<Notification>()
          every { Notification.get(any()) } returns notification
          val rateAction = notification.actions.first { it.templateText == "Later" }
          rateAction.actionPerformed(mockk())
        }
        RateUsNotification.showRateUsNotification()
        assertSoftly { isBrowseTriggered shouldBe false }
        assertSoftly { configService.isRateUsNotificationDelayChanged shouldBe false }
      }
      should("the 'Rate us' notification is appeared and the 'Dismiss' button is clicked") {
        every { Notifications.Bus.notify(any<Notification>()) } answers {
          val notification = firstArg<Notification>()
          every { Notification.get(any()) } returns notification
          val rateAction = notification.actions.first { it.templateText == "Dismiss" }
          rateAction.actionPerformed(mockk())
        }
        RateUsNotification.showRateUsNotification()
        assertSoftly { isBrowseTriggered shouldBe false }
        assertSoftly { configService.isRateUsNotificationDelayChanged shouldBe true }
      }
    }
  }
})
