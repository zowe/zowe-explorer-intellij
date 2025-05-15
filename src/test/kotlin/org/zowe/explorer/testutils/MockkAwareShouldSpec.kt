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

package org.zowe.explorer.testutils

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearAllMocks
import io.mockk.unmockkAll

/**
 * [ShouldSpec] wrapper for test cases with mockk library usage.
 * Automatically clears all mocks created during test cases run
 */
abstract class MockkAwareShouldSpec(body: ShouldSpec.() -> Unit = {}) : ShouldSpec(body) {
  override suspend fun afterSpec(spec: Spec) {
    super.afterSpec(spec)
    unmockkAll()
    clearAllMocks()
  }
}
