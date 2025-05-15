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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.rateus

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import org.zowe.explorer.config.ConfigService
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.testutils.AppInitShouldSpec
import kotlin.reflect.KFunction

class RateUsNotificationTestSpec : AppInitShouldSpec("rateus/RateUsNotification", {
  context("showRateUsNotification") {
    var didChangeRateUsNotificationDelay = false

    // Just to cover the class usage
    RateUsNotification()

    var isBrowseTriggered = false

    val configService = ConfigService.getService()
    every {
      configService.rateUsNotificationDelay = any()
    } answers {
      didChangeRateUsNotificationDelay = true
    }

    val browseRef: (String) -> Unit = BrowserUtil::browse
    val notifyRef: (Notification) -> Unit = Notifications.Bus::notify

    beforeEach {
      didChangeRateUsNotificationDelay = false

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
      assertSoftly { didChangeRateUsNotificationDelay shouldBe true }
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
      assertSoftly { didChangeRateUsNotificationDelay shouldBe false }
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
      assertSoftly { didChangeRateUsNotificationDelay shouldBe true }
    }
  }
})
