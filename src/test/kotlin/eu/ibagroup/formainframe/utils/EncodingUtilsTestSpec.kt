/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.explorer.ui.ChangeEncodingDialog
import eu.ibagroup.formainframe.testServiceImpl.TestDataOpsManagerImpl
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.UnsupportedCharsetException

class EncodingUtilsTestSpec: ShouldSpec({
  beforeSpec {
    // FIXTURE SETUP TO HAVE ACCESS TO APPLICATION INSTANCE
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "for-mainframe")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
  }
  afterSpec {
    clearAllMocks()
  }
  context("utils module: encodingUtils") {

    val text = "text"
    val textBuf = CharBuffer.wrap(text)
    val bytes = byteArrayOf(116, 101, 120, 116)
    val bytesByf = ByteBuffer.wrap(bytes)
    var isEncodingSet = false

    val explorerMock = mockk<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    val contentSynchronizerMock = mockk<ContentSynchronizer>()
    val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    every { contentSynchronizerMock.successfulContentStorage(any()) } returns bytes

    val charsetMock = mockk<Charset>()
    every { charsetMock.name() } returns "charsetName"

    val virtualFileMock = mockk<VirtualFile>()
    every { virtualFileMock.name } returns "fileName"
    every { virtualFileMock.charset = charsetMock } returns Unit
    every { virtualFileMock.charset } returns charsetMock

    mockkConstructor(DocumentedSyncProvider::class)
    every { anyConstructed<DocumentedSyncProvider>().saveDocument() } returns Unit

    val documentMockk = mockk<Document>()
    every { anyConstructed<DocumentedSyncProvider>().retrieveCurrentContent() } returns bytes

    every { documentMockk.text } returns text
    every { documentMockk.modificationStamp } returns 0L

    mockkStatic(EncodingManager::getInstance)
    every { EncodingManager.getInstance() } answers {
      object : TestEncodingManager() {
        override fun setEncoding(virtualFileOrDir: VirtualFile?, charset: Charset?) {
          isEncodingSet = true
          return
        }
      }
    }

    mockkStatic(LoadTextUtil::class)
    every { LoadTextUtil.write(any(), virtualFileMock, virtualFileMock, any(), any()) } returns Unit
    every { LoadTextUtil.getTextByBinaryPresentation(bytes, virtualFileMock) } returns text

    val decoderMock = mockk<CharsetDecoder>()
    every { charsetMock.newDecoder() } returns decoderMock

    val lineSeparator = "\n"
    every { FileDocumentManager.getInstance().getLineSeparator(virtualFileMock, null) } returns lineSeparator

    val encoderMock = mockk<CharsetEncoder>()
    every { charsetMock.newEncoder() } returns encoderMock

    val attributesMock = mockk<RemoteUssAttributes>()

    mockkObject(ChangeEncodingDialog)
    every { ChangeEncodingDialog["initialize"](any<() -> Unit>()) } returns Unit

    mockkConstructor(ChangeEncodingDialog::class)
    every { anyConstructed<ChangeEncodingDialog>().show() } returns Unit

    beforeEach {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }
      }

      every { anyConstructed<DocumentedSyncProvider>().getDocument() } returns documentMockk

      every { decoderMock.decode(any()) } returns textBuf
      every { encoderMock.encode(any()) } returns bytesByf

      isEncodingSet = false
    }

    // saveIn
    should("convert file content to new encoding when file upload is needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      saveIn(null, virtualFileMock, charsetMock)

      assertSoftly { isEncodingSet shouldBe true }
    }
    should("convert file content to new encoding when file upload is not needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false

      saveIn(null, virtualFileMock, charsetMock)

      assertSoftly { isEncodingSet shouldBe true }
    }
    // reloadIn
    should("reload file content to new encoding") {
      every { contentSynchronizerMock.synchronizeWithRemote(any()) } returns Unit

      reloadIn(null, virtualFileMock, charsetMock)

      assertSoftly { isEncodingSet shouldBe true }
    }
    // changeFileEncodingTo
    should("change file encoding to new one") {
      changeFileEncodingTo(virtualFileMock, charsetMock)

      assertSoftly { isEncodingSet shouldBe true }
    }
    // isSafeToReloadIn
    should("check if it is safe to reload into 'ABSOLUTELY' encoding") {
      val actual = isSafeToReloadIn(virtualFileMock, text, bytes, charsetMock)

      val expected = Magic8.ABSOLUTELY

      assertSoftly { actual shouldBe expected }
    }
    should("check if it is safe to reload into 'WELL_IF_YOU_INSIST' encoding") {
      every { decoderMock.decode(any()) } returns CharBuffer.wrap("ÈÁÌÈ")

      val actual = isSafeToReloadIn(virtualFileMock, text, bytes, charsetMock)

      val expected = Magic8.WELL_IF_YOU_INSIST

      assertSoftly { actual shouldBe expected }
    }
    should("check if it is safe to reload into 'NO_WAY' encoding") {
      every { decoderMock.decode(any()) } throws UnsupportedCharsetException("")

      val actual = isSafeToReloadIn(virtualFileMock, text, bytes, charsetMock)

      val expected = Magic8.NO_WAY

      assertSoftly { actual shouldBe expected }
    }
    // isSafeToConvertTo
    should("check if it is safe to convert to 'ABSOLUTELY' encoding") {
      val actual = isSafeToConvertTo(virtualFileMock, text, charsetMock)

      val expected = Magic8.ABSOLUTELY

      assertSoftly { actual shouldBe expected }
    }
    should("check if it is safe to convert to 'NO_WAY' encoding") {
      every { encoderMock.encode(any()) } throws UnsupportedCharsetException("")

      val actual = isSafeToConvertTo(virtualFileMock, text, charsetMock)

      val expected = Magic8.NO_WAY

      assertSoftly { actual shouldBe expected }
    }
    // inspectSafeEncodingChange
    should("inspect safe encoding change when file upload is not needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false

      val actual = inspectSafeEncodingChange(virtualFileMock, charsetMock)

      val expected = EncodingInspection(Magic8.ABSOLUTELY, Magic8.ABSOLUTELY)

      assertSoftly { actual shouldBe expected }
    }
    should("inspect safe encoding change when file upload needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      val actual = inspectSafeEncodingChange(virtualFileMock, charsetMock)

      val expected = EncodingInspection(Magic8.ABSOLUTELY, Magic8.ABSOLUTELY)

      assertSoftly { actual shouldBe expected }
    }
    should("inspect safe encoding change when document is null") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false
      every { anyConstructed<DocumentedSyncProvider>().getDocument() } returns null

      var throwable: Throwable? = null
      runCatching {
        inspectSafeEncodingChange(virtualFileMock, charsetMock)
      }.onFailure {
        throwable = it
      }

      val expected = IllegalArgumentException("Cannot get document text")

      assertSoftly { throwable shouldBe expected }
    }
    should("inspect safe encoding change when content synchronizer is null") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
          return null
        }
      }

      var throwable: Throwable? = null
      runCatching {
        inspectSafeEncodingChange(virtualFileMock, charsetMock)
      }.onFailure {
        throwable = it
      }

      val expected = IllegalArgumentException("Cannot get content bytes")

      assertSoftly { throwable shouldBe expected }
    }
    // changeFileEncodingAction
    should("change file encoding action by reload button") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false
      every { contentSynchronizerMock.isFileSyncPossible(any()) } returns true

      every { anyConstructed<ChangeEncodingDialog>().exitCode } returns ChangeEncodingDialog.RELOAD_EXIT_CODE

      val actual = changeFileEncodingAction(virtualFileMock, attributesMock, charsetMock)

      assertSoftly { actual shouldBe true }
    }
    should("change file encoding action by convert button") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false
      every { contentSynchronizerMock.isFileSyncPossible(any()) } returns true

      every { anyConstructed<ChangeEncodingDialog>().exitCode } returns ChangeEncodingDialog.CONVERT_EXIT_CODE

      val actual = changeFileEncodingAction(virtualFileMock, attributesMock, charsetMock)

      assertSoftly { actual shouldBe true }
    }
    should("change file encoding action by cancel button") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false
      every { contentSynchronizerMock.isFileSyncPossible(any()) } returns true

      every { anyConstructed<ChangeEncodingDialog>().exitCode } returns 1

      val actual = changeFileEncodingAction(virtualFileMock, attributesMock, charsetMock)

      assertSoftly { actual shouldBe false }
    }
    // createCharsetsActionGroup
    should("create charsets action group from supported encodings") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false

      val safeToReloadPossibleAnswers = listOf(Magic8.NO_WAY, Magic8.WELL_IF_YOU_INSIST, Magic8.ABSOLUTELY)
      val safeToConvertPossibleAnswers = listOf(Magic8.NO_WAY, Magic8.ABSOLUTELY)
      mockkStatic(::inspectSafeEncodingChange)
      every { inspectSafeEncodingChange(virtualFileMock, any()) } answers {
        val safeToReloadNum = safeToReloadPossibleAnswers.indices.random()
        val safeToReload = safeToReloadPossibleAnswers[safeToReloadNum]
        val safeToConvertNum = safeToConvertPossibleAnswers.indices.random()
        val safeToConvert = safeToConvertPossibleAnswers[safeToConvertNum]
        EncodingInspection(safeToReload, safeToConvert)
      }

      val actual = createCharsetsActionGroup(virtualFileMock, attributesMock)

      assertSoftly { actual.childrenCount shouldBe 6 }
    }

    unmockkAll()
  }
})