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

package eu.ibagroup.formainframe.testutils

import com.intellij.openapi.application.Application
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.ConfigSandbox
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.service.SyncProcessService
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.testutils.testServiceImpl.*
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearMocks

var testAppFixture: CodeInsightTestFixture? = null

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
    if (testAppFixture == null) {
      val factory = IdeaTestFixtureFactory.getFixtureFactory()
      val lightFixture = factory.createLightFixtureBuilder("for-mainframe").fixture
      testAppFixture = factory
        .createCodeInsightFixture(lightFixture, LightTempDirTestFixtureImpl(true))
      testAppFixture?.setUp() ?: throw Exception("Fixture setup is failed")
    }

    (AnalyticsService.getService() as TestAnalyticsServiceImpl).testInstance = TestAnalyticsServiceImpl()
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
//    testAppFixture.tearDown()
    super.afterSpec(spec)
  }

  init {
    body()
  }
}
