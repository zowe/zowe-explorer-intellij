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

package eu.ibagroup.formainframe.utils

import com.intellij.codeInspection.GlobalInspectionContext
import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.explorer.ui.ChangeEncodingDialog
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.UnsupportedCharsetException
import javax.swing.Icon
import kotlin.reflect.KFunction

class EncodingUtilsTestSpec : WithApplicationShouldSpec({
  context("utils module: encodingUtils") {

    val text = "text"
    val textBuf = CharBuffer.wrap(text)
    val bytes = byteArrayOf(116, 101, 120, 116)
    val bytesByf = ByteBuffer.wrap(bytes)
    var isEncodingSet = false

    val contentSynchronizerMock = mockk<ContentSynchronizer>()
    val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
    every { contentSynchronizerMock.successfulContentStorage(any()) } returns bytes

    val charsetName = "charsetName"
    val charsetMock = mockk<Charset>()
    every { charsetMock.name() } returns charsetName
    every { charsetMock.displayName() } returns charsetName

    val virtualFileMock = mockk<VirtualFile>()
    every { virtualFileMock.name } returns "fileName"
    every { virtualFileMock.charset = charsetMock } returns Unit
    every { virtualFileMock.charset } returns charsetMock
    every { virtualFileMock.getOutputStream(null) } returns mockk {
      every { close() } returns Unit
      every { write(any<ByteArray>()) } returns Unit
    }

    mockkConstructor(DocumentedSyncProvider::class)
    every { anyConstructed<DocumentedSyncProvider>().saveDocument() } returns Unit
    every { anyConstructed<DocumentedSyncProvider>().loadNewContent(any<ByteArray>()) } returns Unit
    every { anyConstructed<DocumentedSyncProvider>().retrieveCurrentContent() } returns bytes

    val documentMockk = mockk<Document>()
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

    val projectMock = mockk<Project>()
    val psiFileMock = mockk<PsiFile>()
    every { PsiManager.getInstance(projectMock).findFile(virtualFileMock) } returns psiFileMock

    val inspectionProfileMock = mockk<InspectionProfileImpl>()
    every { InspectionProjectProfileManager.getInstance(projectMock).currentProfile } returns inspectionProfileMock

    val inspectionToolMock = mockk<InspectionToolWrapper<*, *>>()
    every { inspectionProfileMock.getInspectionTool(any(), projectMock) } returns inspectionToolMock

    val inspectionManagerMock = mockk<InspectionManager>()
    mockkStatic(InspectionManager::getInstance)
    every { InspectionManager.getInstance(projectMock) } returns inspectionManagerMock

    val globalInspectionContextMock = mockk<GlobalInspectionContext>()
    every { inspectionManagerMock.createNewGlobalContext() } returns globalInspectionContextMock

    mockkStatic(InspectionEngine::runInspectionOnFile)

    val showDialogRef: (String, String, Array<String>, Int, Icon) -> Int = Messages::showDialog
    mockkStatic(showDialogRef as KFunction<*>)

    beforeEach {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }
      }

      every { anyConstructed<DocumentedSyncProvider>().getDocument() } returns documentMockk

      every { decoderMock.decode(any()) } returns textBuf
      every { encoderMock.encode(any()) } returns bytesByf

      isEncodingSet = false

      every {
        InspectionEngine.runInspectionOnFile(psiFileMock, inspectionToolMock, globalInspectionContextMock)
      } returns emptyList()
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
      every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } returns Unit

      reloadIn(null, virtualFileMock, charsetMock, mockk())

      assertSoftly { isEncodingSet shouldBe true }
    }
    // changeEncodingTo
    should("change file encoding to new one") {
      changeEncodingTo(virtualFileMock, charsetMock)

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
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
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
      every { attributesMock.isWritable } returns true

      every { anyConstructed<ChangeEncodingDialog>().exitCode } returns ChangeEncodingDialog.RELOAD_EXIT_CODE

      val actual = runBlocking {
        withContext(Dispatchers.EDT) {
          changeFileEncodingAction(projectMock, virtualFileMock, attributesMock, charsetMock)
        }
      }

      assertSoftly { actual shouldBe true }
    }
    should("change file encoding action by convert button") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false
      every { attributesMock.isWritable } returns true

      every { anyConstructed<ChangeEncodingDialog>().exitCode } returns ChangeEncodingDialog.CONVERT_EXIT_CODE

      val actual = runBlocking {
        withContext(Dispatchers.EDT) {
          changeFileEncodingAction(projectMock, virtualFileMock, attributesMock, charsetMock)
        }
      }

      assertSoftly { actual shouldBe true }
    }
    should("change file encoding action by cancel button") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false
      every { attributesMock.isWritable } returns true

      every { anyConstructed<ChangeEncodingDialog>().exitCode } returns 1

      val actual = runBlocking {
        withContext(Dispatchers.EDT) {
          changeFileEncodingAction(projectMock, virtualFileMock, attributesMock, charsetMock)
        }
      }

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
    // checkEncodingCompatibility
    should("check encoding compatibility on save if encoding is compatible") {
      val actual = checkEncodingCompatibility(virtualFileMock, projectMock)

      assertSoftly { actual shouldBe true }
    }
    should("check encoding compatibility on save if encoding is incompatible") {
      every {
        InspectionEngine.runInspectionOnFile(psiFileMock, inspectionToolMock, globalInspectionContextMock)
      } returns listOf(mockk<ProblemDescriptor>())

      val actual = checkEncodingCompatibility(virtualFileMock, projectMock)

      assertSoftly { actual shouldBe false }
    }
    // showSaveAnywayDialog
    should("show save anyway dialog when user clicks save anyway") {
      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 0

      val actual = showSaveAnywayDialog(charsetMock)

      assertSoftly { actual shouldBe true }
    }
    should("show save anyway dialog when user clicks cancel") {
      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 1

      val actual = showSaveAnywayDialog(charsetMock)

      assertSoftly { actual shouldBe false }
    }

    unmockkAll()
  }
})
