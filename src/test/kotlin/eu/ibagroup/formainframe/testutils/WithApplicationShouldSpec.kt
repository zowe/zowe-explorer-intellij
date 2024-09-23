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
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.ConfigSandbox
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.service.SyncProcessService
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestAnalyticsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestConfigSandboxImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestConfigServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestCredentialsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestNotificationsServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestSyncProcessServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestZosmfApiImpl
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearMocks

/**
 * [ShouldSpec] wrapper that provides implemented beforeSpec, initializing an [Application]
 * instance to be able to use and mock services for tests
 */
abstract class WithApplicationShouldSpec(body: ShouldSpec.() -> Unit = {}) : ShouldSpec() {
  private lateinit var appFixture: IdeaProjectTestFixture

  /**
   * Fixture setup to have access to the [Application] instance
   */
  override suspend fun beforeSpec(spec: Spec) {
    super.beforeSpec(spec)
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val lightFixture = factory.createLightFixtureBuilder("for-mainframe").fixture
    appFixture = factory.createCodeInsightFixture(lightFixture)
    appFixture.setUp()

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
//    appFixture.tearDown()
    super.afterSpec(spec)
  }

  init {
    body()
  }
}
