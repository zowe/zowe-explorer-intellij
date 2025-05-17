/*
 * Copyright (c) 2025 IBA Group.
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

package org.zowe.explorer.v3.state.settings

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService
import java.util.UUID

@OptIn(StableStorage::class)
class OtherSettingsServiceTestSpec : AppInitShouldSpec("v3/state/settings/OtherSettingsService", {
  lateinit var currentTestUuid: UUID

  beforeSpec {
    currentTestUuid = AppInitShouldSpec.currentTestUuid ?: throw Exception("Test UUID must be defined before the spec run")
  }

  context("all functions") {
    var otherSettingsReloadCount = 0
    var otherSettingsChangeCount = 0
    var didSettingsUpdatedInStorage = false
    var didSettingsReloadedFromStorage = false

    val otherSettingsService = OtherSettingsService()

    subscribe(
      StorageService.OTHER_SETTINGS_TOPIC,
      object : OtherSettingsEventListener {
        override fun otherSettingsReloaded(newSettings: OtherSettingsHolder) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            didSettingsReloadedFromStorage = true
          }
        }

        override fun otherSettingsChanged(newSettings: OtherSettingsHolder) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            didSettingsUpdatedInStorage = true
          }
        }
      }
    )

    subscribe(
      OtherSettingsService.TOPIC,
      object : OtherSettingsEventListener {
        override fun otherSettingsReloaded(newSettings: OtherSettingsHolder) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            otherSettingsReloadCount += 1
          }
        }

        override fun otherSettingsChanged(newSettings: OtherSettingsHolder) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            otherSettingsChangeCount += 1
          }
        }
      }
    )

    beforeEach {
      otherSettingsReloadCount = 0
      otherSettingsChangeCount = 0
      didSettingsUpdatedInStorage = false
      didSettingsReloadedFromStorage = false
    }

    should("reload other settings when the respective event is arrived") {
      val newSettings = OtherSettingsState()
      sendTopic(StorageService.OTHER_SETTINGS_TOPIC).otherSettingsReloaded(newSettings)

      val result = otherSettingsService.getOtherSettings()
      assertSoftly { result shouldBe newSettings }
      assertSoftly { otherSettingsReloadCount shouldBe 1 }
      assertSoftly { otherSettingsChangeCount shouldBe 0 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe false }
      assertSoftly { didSettingsReloadedFromStorage shouldBe true }
    }

    should("update other settings when the respective event is arrived") {
      val newSettings = OtherSettingsState()
      sendTopic(StorageService.OTHER_SETTINGS_TOPIC).otherSettingsChanged(newSettings)

      val result = otherSettingsService.getOtherSettings()
      assertSoftly { result shouldBe newSettings }
      assertSoftly { otherSettingsReloadCount shouldBe 0 }
      assertSoftly { otherSettingsChangeCount shouldBe 1 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe true }
      assertSoftly { didSettingsReloadedFromStorage shouldBe false }
    }

    should("not update other settings when the respective event is arrived cause the settings are the same") {
      val newSettings = OtherSettingsState()
      sendTopic(StorageService.OTHER_SETTINGS_TOPIC).otherSettingsChanged(newSettings)
      sendTopic(StorageService.OTHER_SETTINGS_TOPIC).otherSettingsChanged(newSettings)

      val result = otherSettingsService.getOtherSettings()
      assertSoftly { result shouldBe newSettings }
      assertSoftly { otherSettingsReloadCount shouldBe 0 }
      assertSoftly { otherSettingsChangeCount shouldBe 1 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe true }
      assertSoftly { didSettingsReloadedFromStorage shouldBe false }
    }

    should("save other settings in storage") {
      otherSettingsService.saveOtherSettings()
      assertSoftly { otherSettingsReloadCount shouldBe 0 }
      assertSoftly { otherSettingsChangeCount shouldBe 0 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe true }
      assertSoftly { didSettingsReloadedFromStorage shouldBe false }
    }

    should("reload other settings from storage") {
      otherSettingsService.reloadOtherSettingsFromStorage()
      assertSoftly { otherSettingsReloadCount shouldBe 1 }
      assertSoftly { otherSettingsChangeCount shouldBe 0 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe false }
      assertSoftly { didSettingsReloadedFromStorage shouldBe true }
    }
  }
})
