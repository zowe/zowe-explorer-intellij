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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.operations.mover

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.LineSeparator
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.changeEncodingTo
import java.io.File
import java.nio.charset.Charset

class RemoteToLocalFileMoverTestSpec : AppInitShouldSpec("dataops/operations/mover/RemoteToLocalFileMover", {
  context("all functions") {
    val remoteToLocalFileMover = spyk(RemoteToLocalFileMover(mockk()))

    val fileMockk = mockk<File>()
    val charsetMockk = mockk<Charset>()
    val virtualFileMockk = mockk<VirtualFile> {
      every { charset } returns charsetMockk
      every { detectedLineSeparator } returns LineSeparator.LF.separatorString
    }

    var encodingChanged = false
    var lineSeparatorChanged = false

    beforeEach {
      encodingChanged = false
      lineSeparatorChanged = false

      mockkStatic(LocalFileSystem::getInstance)
      every { LocalFileSystem.getInstance().refreshAndFindFileByIoFile(fileMockk) } returns mockk {
        every { detectedLineSeparator = any<String>() } answers {
          lineSeparatorChanged = true
        }
      }

      mockkStatic(::changeEncodingTo)
      every {
        changeEncodingTo(any(), charsetMockk)
      } answers {
        encodingChanged = true
      }
    }

    val setCreatedFileParamsRef = remoteToLocalFileMover::class.java
      .declaredMethods
      .first { it.name == "setCreatedFileParams" }
    setCreatedFileParamsRef.trySetAccessible()

    should("set parameters for created file in local system") {
      setCreatedFileParamsRef.invoke(remoteToLocalFileMover, fileMockk, virtualFileMockk)

      assertSoftly {
        encodingChanged shouldBe true
        lineSeparatorChanged shouldBe true
      }

    }
  }
})
