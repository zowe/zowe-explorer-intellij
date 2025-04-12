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

package org.zowe.explorer.zowe.service

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.*
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME

class ZoweConfigServiceImplCompanionTestSpec : ShouldSpec({
  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("zowe/service/ZoweConfigServiceImpl.Companion") {
    var errorNotificationTrigerredCount = 0

    val projectMock = mockk<Project> {
      every { name } returns "test_project_name"
      every { basePath } returns "test/project/base/path"
    }

    val applicationMock = mockk<Application>()
    mockkStatic(ApplicationManager::getApplication)
    every { ApplicationManager.getApplication() } returns applicationMock

    beforeEach {
      errorNotificationTrigerredCount = 0

      every { applicationMock.getService(ConfigService::class.java) } returns mockk {
        every { crudable } returns mockk {
          every {
            getAll(any<Class<out ConnectionConfig>>())
          } answers {
            emptyList<ConnectionConfig>().stream()
          }
        }
      }

      every { applicationMock.getService(NotificationsService::class.java) } returns mockk {
        every {
          notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
        } answers {
          errorNotificationTrigerredCount += 1
        }
      }
    }

    context("ZoweConfigServiceImpl.getZoweConnectionName") {
      should("return a new Zowe connection name both for local and global Zowe Configs") {
        val custProfileName = "test-profile"
        val resultLocalDefaultProfile = ZoweConfigServiceImpl.getZoweConnectionName(projectMock, ZoweConfigType.LOCAL)
        val resultLocalCustomProfile = ZoweConfigServiceImpl.getZoweConnectionName(projectMock, ZoweConfigType.LOCAL, custProfileName)
        val resultGlobal = ZoweConfigServiceImpl.getZoweConnectionName(null, ZoweConfigType.GLOBAL)

        assertSoftly { resultLocalDefaultProfile shouldBe "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}" }
        assertSoftly { resultLocalCustomProfile shouldBe "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$custProfileName/${projectMock.name}" }
        assertSoftly { resultGlobal shouldBe "$ZOWE_PROJECT_PREFIX${ZoweConfigType.GLOBAL}-zosmf" }
        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
      }

      should("return an empty string when there is no project for the local Zowe Config") {
        val result = ZoweConfigServiceImpl.getZoweConnectionName(null, ZoweConfigType.LOCAL)

        assertSoftly { result shouldBe "" }
        assertSoftly { errorNotificationTrigerredCount shouldBe 1 }
      }

      should("return a new Zowe connection name when there is a duplicating connection name already saved") {
        every { applicationMock.getService(ConfigService::class.java) } returns mockk {
          every { crudable } returns mockk {
            every {
              getAll(any<Class<out ConnectionConfig>>())
            } answers {
              listOf<ConnectionConfig>(
                mockk { every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.GLOBAL}-zosmf" },
                mockk { every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}" },
                mockk { every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}1" },
                mockk { every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}2" }
              ).stream()
            }
          }
        }

        val result = ZoweConfigServiceImpl.getZoweConnectionName(projectMock, ZoweConfigType.LOCAL)

        assertSoftly { result shouldBe "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}3" }
        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
      }
    }

    context("ZoweConfigServiceImpl.getZoweConfigLocation") {
      should("return both local and global Zowe Config paths on request") {
        val resultLocalConfig = ZoweConfigServiceImpl.getZoweConfigLocation(projectMock, ZoweConfigType.LOCAL)
        val resultGlobalConfig = ZoweConfigServiceImpl.getZoweConfigLocation(null, ZoweConfigType.GLOBAL)

        assertSoftly { resultLocalConfig shouldBe "${projectMock.basePath}/$ZOWE_CONFIG_NAME" }
        assertSoftly { resultGlobalConfig shouldNotContain "${projectMock.basePath}" }
        assertSoftly { resultGlobalConfig shouldContain "/.zowe/$ZOWE_CONFIG_NAME" }
        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
      }

      should("return an empty string when there is no project for the local Zowe Config") {
        val result = ZoweConfigServiceImpl.getZoweConfigLocation(null, ZoweConfigType.LOCAL)

        assertSoftly { result shouldBe "" }
        assertSoftly { errorNotificationTrigerredCount shouldBe 1 }
      }
    }

    context("ZoweConfigServiceImpl.getProfileNameFromConnName") {
      should("recognize a profile name both from local and global Zowe Configs") {
        val profileName = "test_profile_name"
        val localConfigConnName = "${ZOWE_PROJECT_PREFIX}${ZoweConfigType.LOCAL}-$profileName/${projectMock.name}"
        val globalConfigConnName = "${ZOWE_PROJECT_PREFIX}${ZoweConfigType.LOCAL}-$profileName"

        val resultLocalConfig = ZoweConfigServiceImpl.getProfileNameFromConnName(localConfigConnName)
        val resultGlobalConfig = ZoweConfigServiceImpl.getProfileNameFromConnName(globalConfigConnName)

        assertSoftly { resultLocalConfig shouldBe profileName }
        assertSoftly { resultGlobalConfig shouldBe profileName }
      }
    }
  }
})