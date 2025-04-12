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

package org.zowe.explorer.editor.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.Language
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.properties.charset.Native2AsciiCharset
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.util.containers.ContainerUtil
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.vfs.MFVirtualFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult
import kotlin.reflect.KFunction

class MFLossyEncodingInspectionTestSpec : AppInitShouldSpec("editor/inspection/MFLossyEncodingInspection", {
  context("checkFile") {
    lateinit var text: CharSequence
    lateinit var lossyEncodingInspection: MFLossyEncodingInspection
    val isOnTheFly = true

    val languageMock = mockk<Language>()
    val viewProviderMock = mockk<FileViewProvider> {
      every { baseLanguage } returns languageMock
    }
    val virtualFileMock = mockk<MFVirtualFile>()
    var attributesMock: FileAttributes
    val coderResultMock = mockk<CoderResult> {
      every { length() } returns 1
    }
    val encoderMock = mockk<CharsetEncoder> {
      every { averageBytesPerChar() } returns 1.0f
      every { flush(any()) } returns coderResultMock
    }
    val decoderMock = mockk<CharsetDecoder>()
    val charsetMock = mockk<Charset> {
      every { newEncoder() } returns mockk {
        every { onUnmappableCharacter(any()) } returns mockk {
          every { onMalformedInput(any()) } returns encoderMock
        }
      }
      every { newDecoder() } returns mockk {
        every { onUnmappableCharacter(any()) } returns mockk {
          every { onMalformedInput(any()) } returns decoderMock
        }
      }
    }
    val decoderResultMock = mockk<CoderResult> {
      every { length() } returns 1
    }

    val getLastItemRef: (MutableList<Any>) -> Any = ContainerUtil::getLastItem
    mockkStatic(getLastItemRef as KFunction<*>)
    every { ContainerUtil.getLastItem(any<MutableList<ProblemDescriptor>>()) } answers {
      val descriptors = firstArg<MutableList<ProblemDescriptor>>()
      if (descriptors.isNotEmpty()) descriptors.last() else null
    }

    val projectMock = mockk<Project>()
    val psiFileMock = mockk<PsiFile> {
      every { viewProvider } returns viewProviderMock
      every { isValid } returns true
    }
    every { psiFileMock.containingFile } returns psiFileMock

    every {
      SmartPointerManager.getInstance(projectMock)
        .createSmartPsiElementPointer<PsiElement>(psiFileMock, psiFileMock)
    } returns mockk()

    val inspectionManagerMock = mockk<InspectionManager>()

    val commonPrefixLengthRef: (CharSequence, CharSequence) -> Int = StringUtil::commonPrefixLength

    val dataOpsManagerService = DataOpsManager.getService()

    beforeEach {
      every { psiFileMock.project } returns projectMock
      every { psiFileMock.isPhysical } returns true
      every { psiFileMock.language } returns languageMock
      every { psiFileMock.virtualFile } returns virtualFileMock
      every { InjectedLanguageManager.getInstance(projectMock).isInjectedFragment(psiFileMock) } returns false

      every { viewProviderMock.contents } answers { text }

      every { LoadTextUtil.extractCharsetFromFileContent(projectMock, virtualFileMock, mockk()) } returns charsetMock

      attributesMock = mockk<RemoteUssAttributes>()

      every { dataOpsManagerService.tryToGetAttributes(any<VirtualFile>()) } returns attributesMock

      every { encoderMock.encode(any(), any(), any()) } returns coderResultMock
      every { coderResultMock.isUnderflow } returns false
      every { coderResultMock.isOverflow } returns false
      every { coderResultMock.isError } returns false

      every { decoderResultMock.isError } returns false

      every {
        inspectionManagerMock
          .createProblemDescriptor(
            psiFileMock,
            any<TextRange>(),
            any<String>(),
            any<ProblemHighlightType>(),
            isOnTheFly,
            *anyVararg()
          )
      } answers {
        val errRange = secondArg<TextRange>()
        val problemDescriptorMock = mockk<ProblemDescriptor>()
        every { problemDescriptorMock.textRangeInElement } answers { errRange }

        problemDescriptorMock
      }

      unmockkStatic(commonPrefixLengthRef as KFunction<*>)
      unmockkStatic(CharBuffer::class)

      lossyEncodingInspection = spyk()
    }

    should("check file if it is injected") {
      every { InjectedLanguageManager.getInstance(projectMock).isInjectedFragment(psiFileMock) } returns true

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors shouldBe null }
    }

    should("check file if it is not physical") {
      every { psiFileMock.isPhysical } returns false

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors shouldBe null }
    }

    should("check file if base language is not file language") {
      every { psiFileMock.language } returns mockk()

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors shouldBe null }
    }

    should("check file if virtual file is null") {
      every { psiFileMock.virtualFile } returns null

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors shouldBe null }
    }

    should("check file if virtual file is not MF virtual file") {
      every { psiFileMock.virtualFile } returns mockk<VirtualFile>()

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors shouldBe null }
    }

    should("check file if charset is native ASCII charset") {
      text = "qwerty"

      every {
        LoadTextUtil.extractCharsetFromFileContent(
          projectMock,
          virtualFileMock,
          mockk()
        )
      } returns mockk<Native2AsciiCharset>()

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors shouldBe null }
    }

    should("check file where all characters are incompatible") {

      text = "текст"

      every { coderResultMock.isError } returns true

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.size shouldBe 1 }
      assertSoftly { descriptors?.get(0)?.textRangeInElement shouldBe TextRange(0, 5) }
    }

    should("check file where all characters are compatible") {
      text = "qwerty"

      every {
        decoderMock.decode(any(), any(), any())
      } answers {
        val back = secondArg<CharBuffer>()
        back.put(text.toString())
        decoderResultMock
      }

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.isEmpty() shouldBe true }
    }

    should("check file that contains incompatible characters") {
      text = "qwerty\nтекст\nqwerty\nтекст"

      every {
        encoderMock.encode(any(), any(), any())
      } answers {
        val input = firstArg<CharBuffer>()
        val pos = input.toString().indexOfFirst { it in 'А'..'я' }
        input.position(input.position() + pos)
        coderResultMock
      }

      every { coderResultMock.isError } returns true

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.size shouldBe 2 }
      assertSoftly { descriptors?.get(0)?.textRangeInElement shouldBe TextRange(7, 12) }
      assertSoftly { descriptors?.get(1)?.textRangeInElement shouldBe TextRange(20, 25) }
    }

    should("check file when coder result if overflow") {
      text = "qwerty..."

      var wasOverflow = false
      every {
        coderResultMock.isOverflow
      } answers {
        if (!wasOverflow) {
          wasOverflow = true
          true
        } else false
      }
      every {
        decoderMock.decode(any(), any(), any())
      } answers {
        val back = secondArg<CharBuffer>()
        back.put(text.toString())
        decoderResultMock
      }

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.isEmpty() shouldBe true }
    }

    should("check file when coder result is underflow") {
      text = "qwerty..."

      val bufferMock = mockk<CharBuffer>()
      mockkStatic(CharBuffer::class)
      every { CharBuffer.wrap(text) } returns bufferMock
      every { bufferMock.limit() } returns text.length
      every { bufferMock.rewind() } returns bufferMock
      every { bufferMock.position(any()) } returns bufferMock
      every { bufferMock.hasRemaining() } returns false

      every { coderResultMock.isUnderflow } returns true

      every {
        decoderMock.decode(any(), any(), any())
      } answers {
        val back = secondArg<CharBuffer>()
        back.put(text.toString())
        decoderResultMock
      }

      mockkStatic(commonPrefixLengthRef as KFunction<*>)
      every { StringUtil.commonPrefixLength(any(), any()) } returns text.length

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.isEmpty() shouldBe true }
    }

    should("check file where not all characters are decoded back") {
      text = "qwerty."

      every {
        decoderMock.decode(any(), any(), any())
      } answers {
        val back = secondArg<CharBuffer>()
        back.position(text.length - 1)
        decoderResultMock
      }

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.size shouldBe 1 }
      assertSoftly { descriptors?.get(0)?.textRangeInElement shouldBe TextRange(6, 7) }
    }

    should("check file that contains more than 200 errors") {
      text = mockk<CharSequence>()
      every { viewProviderMock.contents } returns text
      every { text.length } returns 201

      var counter = 0
      every {
        lossyEncodingInspection["nextUnmappable"](
          any<CharBuffer>(), any<Int>(), any<Ref<ByteBuffer>>(), any<CharBuffer>(), charsetMock
        )
      } answers {
        val errRange = TextRange(counter, counter + 1)
        counter++
        errRange
      }

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.size shouldBe 1 }
      assertSoftly { descriptors?.get(0)?.textRangeInElement shouldBe TextRange(0, 200) }
    }

    should("check file if attributes are not uss attributes") {
      text = "текст"

      attributesMock = mockk<RemoteMemberAttributes>()
      every { coderResultMock.isError } returns true

      every {
        inspectionManagerMock.createProblemDescriptor(
          psiFileMock,
          any<TextRange>(),
          any<String>(),
          any<ProblemHighlightType>(),
          isOnTheFly,
          *anyVararg()
        )
      } answers {
        val errRange = secondArg<TextRange>()
        val problemDescriptorMock = mockk<ProblemDescriptor>()
        every { problemDescriptorMock.textRangeInElement } answers { errRange }
        every { problemDescriptorMock.fixes } returns emptyArray()

        problemDescriptorMock
      }

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.size shouldBe 1 }
      assertSoftly { descriptors?.get(0)?.fixes?.size shouldBe 0 }
    }

    should("check file when decoded back characters are not equal to input characters") {
      text = "qwerty."

      every { decoderMock.decode(any(), any(), any()) } answers {
        val back = secondArg<CharBuffer>()
        back.put(text.toString())
        decoderResultMock
      }

      mockkStatic(commonPrefixLengthRef as KFunction<*>)
      every { StringUtil.commonPrefixLength(any(), any()) } returns text.length - 1

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.size == 1 }
      assertSoftly { descriptors?.get(0)?.textRangeInElement shouldBe TextRange(6, 7) }
    }
  }
})
