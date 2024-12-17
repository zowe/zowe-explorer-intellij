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
 */

package org.zowe.explorer.dataops.content.adapters

import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.unmockkAll

class SeqDatasetContentAdapterTestSpec: WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("content/adapters: SeqDatasetContentAdapter") {
    val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
    val classUnderTest = spyk(SeqDatasetContentAdapter(dataOpsManager), "SeqDatasetContentAdapter")

    should("shouldReturnAdaptedContent_whenAdaptWhitespaces_givenContentToAdapt") {
      val contentToAdapt = "This is a test string.\n   Content should be replaced by   \n" +
          "this content without trailing               \n" +
          "\n" +
          "   whitespaces..."
      val expected = "This is a test string.\n   Content should be replaced by\n" +
          "this content without trailing\n" +
          "\n" +
          "   whitespaces..."
      val adaptedContent = classUnderTest.adaptWhitespaces(contentToAdapt)

      assertSoftly {
        adaptedContent shouldBe expected
      }
    }
  }
})