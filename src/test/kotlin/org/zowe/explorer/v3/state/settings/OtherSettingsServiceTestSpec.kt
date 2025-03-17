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
 */

package org.zowe.explorer.v3.state.settings

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

@OptIn(StableStorage::class)
class OtherSettingsServiceTestSpec : ShouldSpec({
  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("v3/state/settings/OtherSettingsService") {
    var otherSettingsReloadCount = 0
    var otherSettingsChangeCount = 0
    var didSettingsUpdatedInStorage = false
    var didSettingsReloadedFromStorage = false
    lateinit var subscriptionObj: OtherSettingsEventListener

    lateinit var otherSettingsService: OtherSettingsService

    beforeEach {
      otherSettingsReloadCount = 0
      otherSettingsChangeCount = 0
      didSettingsUpdatedInStorage = false
      didSettingsReloadedFromStorage = false

      // Needed or companion object initialization
      StorageService.Companion
      val applicationMockk = mockk<Application> {
        every { getService(StorageService::class.java) } returns mockk {
          every {
            updateSettingsInStorage(any())
          } answers {
            didSettingsUpdatedInStorage = true
          }
          every {
            reloadOtherSettingsFromStorage()
          } answers {
            didSettingsReloadedFromStorage = true
          }
        }
        every { messageBus } returns mockk {
          every { connect() } returns mockk {
            every {
              subscribe(any<Topic<OtherSettingsEventListener>>(), any<OtherSettingsEventListener>())
            } answers {
              subscriptionObj = secondArg<OtherSettingsEventListener>()
            }
          }
          every {
            syncPublisher(any<Topic<*>>())
          } answers {
            val topic = value as Topic<*>
            if (topic.displayName.contains("OtherSettingsEventListener")) {
              val otherSettingsEventListener = value.castOrNull<Topic<OtherSettingsEventListener>>()
              if (otherSettingsEventListener != null) {
                mockk<OtherSettingsEventListener> {
                  every {
                    otherSettingsReloaded(any<OtherSettingsHolder>())
                  } answers {
                    otherSettingsReloadCount += 1
                  }
                  every {
                    otherSettingsChanged(any<OtherSettingsHolder>())
                  } answers {
                    otherSettingsChangeCount += 1
                  }
                }
              } else {
                fail("Topic is impossible to cast to Topic<OtherSettingsEventListener>")
              }
            } else {
              fail("Unrecognized event listener")
            }
          }
        }
      }

      mockkStatic(ApplicationManager::getApplication)
      every { ApplicationManager.getApplication() } returns applicationMockk

      otherSettingsService = OtherSettingsService()

      every { applicationMockk.getService(OtherSettingsService::class.java) } returns otherSettingsService
    }

    should("reload other settings when the respective event is arrived") {
      val newSettings = OtherSettingsState()
      subscriptionObj.otherSettingsReloaded(newSettings)

      val result = otherSettingsService.getOtherSettings()
      assertSoftly { result shouldBe newSettings }
      assertSoftly { otherSettingsReloadCount shouldBe 1 }
      assertSoftly { otherSettingsChangeCount shouldBe 0 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe false }
      assertSoftly { didSettingsReloadedFromStorage shouldBe false }
    }

    should("update other settings when the respective event is arrived") {
      val newSettings = OtherSettingsState()
      subscriptionObj.otherSettingsChanged(newSettings)

      val result = otherSettingsService.getOtherSettings()
      assertSoftly { result shouldBe newSettings }
      assertSoftly { otherSettingsReloadCount shouldBe 0 }
      assertSoftly { otherSettingsChangeCount shouldBe 1 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe false }
      assertSoftly { didSettingsReloadedFromStorage shouldBe false }
    }

    should("not update other settings when the respective event is arrived cause the settings are the same") {
      val newSettings = OtherSettingsState()
      subscriptionObj.otherSettingsChanged(newSettings)
      subscriptionObj.otherSettingsChanged(newSettings)

      val result = otherSettingsService.getOtherSettings()
      assertSoftly { result shouldBe newSettings }
      assertSoftly { otherSettingsReloadCount shouldBe 0 }
      assertSoftly { otherSettingsChangeCount shouldBe 1 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe false }
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
      assertSoftly { otherSettingsReloadCount shouldBe 0 }
      assertSoftly { otherSettingsChangeCount shouldBe 0 }
      assertSoftly { didSettingsUpdatedInStorage shouldBe false }
      assertSoftly { didSettingsReloadedFromStorage shouldBe true }
    }
  }
})
