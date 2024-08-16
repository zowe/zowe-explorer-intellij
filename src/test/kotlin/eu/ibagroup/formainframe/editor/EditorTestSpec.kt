/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.editor

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.Language
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.properties.charset.Native2AsciiCharset
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.isComponentUnderMouse
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.service.isFileSyncingNow
import eu.ibagroup.formainframe.dataops.content.synchronizer.AutoSyncFileListener
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.editor.inspection.MFLossyEncodingInspection
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.checkEncodingCompatibility
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.utils.showSaveAnywayDialog
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import java.awt.Component
import java.awt.Point
import java.awt.event.FocusEvent
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.reflect.KFunction

class EditorTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("editor module: FileEditorBeforeEventsListener") {
    // beforeFileClosed
    should("perform file sync before it is closed") {}
    should("not perform file sync before it is closed as it is already synced") {}
  }
  context("editor module: FileEditorFocusListener") {

    val fileEditorFocusListener = FileEditorFocusListener()

    val editorMock = mockk<EditorEx>()
    val editorComponentMock = mockk<JComponent>()
    every { editorMock.component } returns editorComponentMock

    mockkStatic(Component::isComponentUnderMouse)

    val focusEventMock = mockk<FocusEvent>()
    val oppositeComponentMock = mockk<Component>()
    every { focusEventMock.oppositeComponent } returns oppositeComponentMock

    val point = Point(0, 0)
    every { oppositeComponentMock.locationOnScreen } returns point

    mockkStatic(SwingUtilities::convertPointFromScreen)
    every { SwingUtilities.convertPointFromScreen(point, editorComponentMock) } returns Unit

    mockkObject(ConfigService)

    val projectMock = mockk<Project>()
    val virtualFileMock = mockk<MFVirtualFile>()
    val explorerMock = mockk<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    val contentSynchronizerMock = mockk<ContentSynchronizer>()
    val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    val bytes = byteArrayOf(116, 101, 120, 116)
    every { contentSynchronizerMock.successfulContentStorage(any()) } returns bytes

    every { virtualFileMock.name } returns "fileName"

    var currentBytes: ByteArray

    mockkConstructor(DocumentedSyncProvider::class)
    every { anyConstructed<DocumentedSyncProvider>().saveDocument() } returns Unit

    mockkStatic(::checkEncodingCompatibility)

    mockkStatic(::showSaveAnywayDialog)

    val charsetMock = mockk<Charset>()
    every { virtualFileMock.charset } returns charsetMock

    var isSynced = false
    val sendTopicRef: (Topic<AutoSyncFileListener>, Project) -> AutoSyncFileListener = ::sendTopic
    mockkStatic(sendTopicRef as KFunction<*>)
    every { sendTopic(AutoSyncFileListener.AUTO_SYNC_FILE, any<Project>()) } answers {
      isSynced = true
      val autoSyncFileListenerMock = mockk<AutoSyncFileListener>()
      every { autoSyncFileListenerMock.sync(virtualFileMock) } returns Unit
      autoSyncFileListenerMock
    }

    mockkStatic(::isFileSyncingNow)

    beforeEach {
      every { editorComponentMock.isComponentUnderMouse() } returns false
      every { editorComponentMock.contains(any()) } returns false

      every { ConfigService.instance.isAutoSyncEnabled } returns true

      every { editorMock.project } returns projectMock
      every { editorMock.virtualFile } returns virtualFileMock

      every { virtualFileMock.isWritable } returns true

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }
      }
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      currentBytes = byteArrayOf(116, 101, 120, 116)
      every { anyConstructed<DocumentedSyncProvider>().retrieveCurrentContent() } answers { currentBytes }

      every { checkEncodingCompatibility(virtualFileMock, projectMock) } returns true

      isSynced = false
    }

    // FileEditorFocusListener.focusLost
    should("perform auto file sync when focus is lost") {
      currentBytes = byteArrayOf(116, 101, 120, 116, 33)

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe true }

    }
    should("perform auto file sync when focus is lost and encoding is incompatible but user saves anyway") {
      currentBytes = byteArrayOf(116, 101, 120, 116, 33)

      every { checkEncodingCompatibility(virtualFileMock, projectMock) } returns false
      every { showSaveAnywayDialog(charsetMock) } returns true

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe true }
    }
    should("not perform auto file sync when focus is lost and encoding is incompatible") {
      currentBytes = byteArrayOf(116, 101, 120, 116, 33)

      every { checkEncodingCompatibility(virtualFileMock, projectMock) } returns false
      every { showSaveAnywayDialog(charsetMock) } returns false

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when focus it lost as it is already synced") {
      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when focus lost by click in editor") {
      every { editorComponentMock.isComponentUnderMouse() } returns true

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when focus lost on component under the editor") {
      every { editorComponentMock.contains(any()) } returns true

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when auto sync is disabled") {
      every { ConfigService.instance.isAutoSyncEnabled } returns false

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when project is null") {
      every { editorMock.project } returns null

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when virtual file is not MF virtual file") {
      every { editorMock.virtualFile } returns mockk<VirtualFile>()

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when virtual file is not writable") {
      every { virtualFileMock.isWritable } returns false

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when content synchronizer is null") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
          return null
        }
      }

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when file upload is not needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    // TODO: Denis fix
    // should("not perform auto file sync when file is syncing now") {
    //   currentBytes = byteArrayOf(116, 101, 120, 116, 33)

    //   every { isFileSyncingNow(any()) } returns true

    //   fileEditorFocusListener.focusLost(editorMock, focusEventMock)

    //   assertSoftly { isSynced shouldBe false }
    // }

    unmockkAll()
  }
  context("editor module: inspection") {

    lateinit var text: CharSequence
    lateinit var lossyEncodingInspection: MFLossyEncodingInspection
    val isOnTheFly = true

    val psiFileMock = mockk<PsiFile>()
    val viewProviderMock = mockk<FileViewProvider>()

    every { psiFileMock.viewProvider } returns viewProviderMock

    val virtualFileMock = mockk<MFVirtualFile>()
    val charsetMock = mockk<Charset>()

    val languageMock = mockk<Language>()
    every { viewProviderMock.baseLanguage } returns languageMock

    val explorerMock = mockk<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    var attributesMock: FileAttributes
    val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl

    val encoderMock = mockk<CharsetEncoder>()
    every { charsetMock.newEncoder().onUnmappableCharacter(any()).onMalformedInput(any()) } returns encoderMock
    every { encoderMock.averageBytesPerChar() } returns 1.0f

    val coderResultMock = mockk<CoderResult>()
    every { encoderMock.flush(any()) } returns coderResultMock
    every { coderResultMock.length() } returns 1

    val decoderMock = mockk<CharsetDecoder>()
    every { charsetMock.newDecoder().onUnmappableCharacter(any()).onMalformedInput(any()) } returns decoderMock

    val decoderResultMock = mockk<CoderResult>()
    every { decoderResultMock.length() } returns 1

    val getLastItemRef: (MutableList<Any>) -> Any = ContainerUtil::getLastItem
    mockkStatic(getLastItemRef as KFunction<*>)
    every { ContainerUtil.getLastItem(any<MutableList<ProblemDescriptor>>()) } answers {
      val descriptors = firstArg<MutableList<ProblemDescriptor>>()
      if (descriptors.isNotEmpty()) descriptors.last() else null
    }

    val projectMock = mockk<Project>()
    every { psiFileMock.isValid } returns true
    every { psiFileMock.containingFile } returns psiFileMock
    every { psiFileMock.project } returns projectMock

    every {
      SmartPointerManager.getInstance(projectMock)
        .createSmartPsiElementPointer<PsiElement>(psiFileMock, psiFileMock)
    } returns mockk()

    val inspectionManagerMock = mockk<InspectionManager>()

    beforeEach {
      every { psiFileMock.project } returns projectMock
      every { psiFileMock.isPhysical } returns true
      every { psiFileMock.language } returns languageMock
      every { psiFileMock.virtualFile } returns virtualFileMock
      every { InjectedLanguageManager.getInstance(projectMock).isInjectedFragment(psiFileMock) } returns false

      every { viewProviderMock.contents } answers { text }

      every { LoadTextUtil.extractCharsetFromFileContent(projectMock, virtualFileMock, mockk()) } returns charsetMock

      attributesMock = mockk<RemoteUssAttributes>()

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
          return attributesMock
        }
      }

      every { encoderMock.encode(any(), any(), any()) } returns coderResultMock
      every { coderResultMock.isUnderflow } returns false
      every { coderResultMock.isOverflow } returns false
      every { coderResultMock.isError } returns false

      every { decoderResultMock.isError } returns false

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

        problemDescriptorMock
      }

      lossyEncodingInspection = spyk()
    }

    // MFLossyEncodingInspection.checkFile
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

      every { decoderMock.decode(any(), any(), any()) } answers {
        val back = secondArg<CharBuffer>()
        back.put(text.toString())
        decoderResultMock
      }

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.isEmpty() shouldBe true }
    }
    should("check file that contains incompatible characters") {
      text = "qwerty\nтекст\nqwerty\nтекст"

      every { encoderMock.encode(any(), any(), any()) } answers {
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
      every { coderResultMock.isOverflow } answers {
        if (!wasOverflow) {
          wasOverflow = true
          true
        } else false
      }
      every { decoderMock.decode(any(), any(), any()) } answers {
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

      every { decoderMock.decode(any(), any(), any()) } answers {
        val back = secondArg<CharBuffer>()
        back.put(text.toString())
        decoderResultMock
      }

      val commonPrefixLengthRef: (CharSequence, CharSequence) -> Int = StringUtil::commonPrefixLength
      mockkStatic(commonPrefixLengthRef as KFunction<*>)
      every { StringUtil.commonPrefixLength(any(), any()) } returns text.length

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      unmockkStatic(CharBuffer::class)
      unmockkStatic(commonPrefixLengthRef)

      assertSoftly { descriptors?.isEmpty() shouldBe true }
    }
    should("check file when getting decoder error") {

      text = "qwe��rty"

      var decoderResultIsError = false
      var pointer = 0
      every { decoderMock.decode(any(), any(), any()) } answers {
        val back = secondArg<CharBuffer>()
        val str = text.substring(pointer)
        val pos = str.indexOfFirst { it !in 'A'..'z' }
        if (pos != -1) {
          decoderResultIsError = true
          pointer += pos
          back.position(pointer)
          pointer++
        } else {
          decoderResultIsError = false
          back.put(str)
        }
        decoderResultMock
      }
      every { decoderResultMock.isError } answers { decoderResultIsError }

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      assertSoftly { descriptors?.size shouldBe 1 }
      assertSoftly { descriptors?.get(0)?.textRangeInElement shouldBe TextRange(3, 5) }
    }
    should("check file where not all characters are decoded back") {

      text = "qwerty."

      every { decoderMock.decode(any(), any(), any()) } answers {
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

      val commonPrefixLengthRef: (CharSequence, CharSequence) -> Int = StringUtil::commonPrefixLength
      mockkStatic(commonPrefixLengthRef as KFunction<*>)
      every { StringUtil.commonPrefixLength(any(), any()) } returns text.length - 1

      val descriptors = lossyEncodingInspection.checkFile(psiFileMock, inspectionManagerMock, isOnTheFly)

      unmockkStatic(commonPrefixLengthRef)

      assertSoftly { descriptors?.size == 1 }
      assertSoftly { descriptors?.get(0)?.textRangeInElement shouldBe TextRange(6, 7) }
    }

    unmockkAll()
  }
})
