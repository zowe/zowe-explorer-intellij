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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.content.adapters

import org.zowe.explorer.dataops.DataOpsManager
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import org.zowe.explorer.testutils.AppInitShouldSpec

class MemberContentAdapterTestSpec : AppInitShouldSpec("dataops/content/adapters/MemberContentAdapter", {
  context("adaptWhitespaces") {
    val dataOpsManager = DataOpsManager.getService()
    val classUnderTest = spyk(MemberContentAdapter(dataOpsManager), "MemberContentAdapter")

    should("return adapted content when with the correct whitespaces distribution") {
      val contentToAdapt = "This is a test string.\n   Content should be replaced by   \n" +
          "this content without trailing               \n" +
          "\n" +
          "   whitespaces..."
      val expected = "This is a test string.\n   Content should be replaced by\n" +
          "this content without trailing\n" +
          "\n" +
          "   whitespaces..."

      val adaptedContent = classUnderTest.adaptWhitespaces(contentToAdapt)

      assertSoftly { adaptedContent shouldBe expected }
    }
  }
})
