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

package org.zowe.explorer.testutils

import com.intellij.openapi.application.Application
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.ConfigSandbox
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.content.service.SyncProcessService
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.testServiceImpl.*
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.*
import org.zowe.explorer.zowe.ZoweStartupActivity
import org.zowe.kotlinsdk.zowe.config.DefaultKeytarWrapper

private var appFixture: CodeInsightTestFixture? = null

/**
 * [ShouldSpec] wrapper that provides implemented beforeSpec, initializing an [Application]
 * instance to be able to use and mock services for tests
 */
abstract class WithApplicationShouldSpec(body: ShouldSpec.() -> Unit = {}) : ShouldSpec() {

  /**
   * Fixture setup to have access to the [Application] instance
   */
  override suspend fun beforeSpec(spec: Spec) {
    super.beforeSpec(spec)
    if (appFixture == null) {
      val factory = IdeaTestFixtureFactory.getFixtureFactory()
      val lightFixture = factory.createLightFixtureBuilder("zowe-explorer").fixture
      appFixture = factory
        .createCodeInsightFixture(lightFixture, LightTempDirTestFixtureImpl(true))
      appFixture?.setUp() ?: throw Exception("Fixture setup is failed")
    }

    (ConfigSandbox.getService() as TestConfigSandboxImpl).testInstance = TestConfigSandboxImpl()
    clearMocks(ConfigSandbox.getService().crudable)

    val configService = ConfigService.getService() as TestConfigServiceImpl
    configService.testInstance = TestConfigServiceImpl()
    clearMocks(configService.crudable)
    configService.resetTestService()

    (CredentialService.getService() as TestCredentialsServiceImpl).testInstance = TestCredentialsServiceImpl()
    (DataOpsManager.getService() as TestDataOpsManagerImpl).testInstance = TestDataOpsManagerImpl()
    (NotificationsService.getService() as TestNotificationsServiceImpl).testInstance = TestNotificationsServiceImpl()
    (SyncProcessService.getService() as TestSyncProcessServiceImpl).testInstance = TestSyncProcessServiceImpl()
    (ZosmfApi.getService() as TestZosmfApiImpl).testInstance = TestZosmfApiImpl()
  }

  override suspend fun afterSpec(spec: Spec) {
    // TODO: figure out, why it does not work properly
//    appFixture.tearDown()
    super.afterSpec(spec)
  }

  init {
    body()
    // TODO: rework
    mockkConstructor(ZoweStartupActivity::class)
    every { anyConstructed<ZoweStartupActivity>().runActivity(any()) } just Runs
  }
}
