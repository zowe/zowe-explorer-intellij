/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.testutils

import com.intellij.openapi.application.Application
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec

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
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val lightFixture = factory.createLightFixtureBuilder("zowe-explorer").fixture
    val appFixture = factory.createCodeInsightFixture(lightFixture)
    appFixture.setUp()
  }

  init {
    body()
  }
}
