/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.*
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.awt.event.ActionEvent
import java.nio.charset.Charset
import javax.swing.Action

class ChangeEncodingDialogTestSpec : ShouldSpec({
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
  context("Explorer module: ui/ChangeEncodingDialog") {

    lateinit var changeEncodingDialog: ChangeEncodingDialog
    var expectedExitCode = 0

    val fileName = "fileName"
    val virtualFileMock = mockk<VirtualFile>()
    every { virtualFileMock.name } returns fileName

    val attributesMock = mockk<RemoteUssAttributes>()

    val charsetName = "charsetName"
    val charsetMock = mockk<Charset>()
    every { charsetMock.name() } returns charsetName

    val safeToReload = Magic8.NO_WAY
    val safeToConvert = Magic8.NO_WAY

    val explorerMock = mockk<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    val contentSynchronizerMock = mockk<ContentSynchronizer>()
    val dataOpsManagerService =
      ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl

    mockkConstructor(DocumentedSyncProvider::class)
    every { anyConstructed<DocumentedSyncProvider>().saveDocument() } returns Unit

    val documentMockk = mockk<Document>()
    every { anyConstructed<DocumentedSyncProvider>().getDocument() } returns documentMockk

    mockkObject(ChangeEncodingDialog)
    every { ChangeEncodingDialog["initialize"](any<() -> Unit>()) } returns Unit

    val actionEventMock = mockk<ActionEvent>()

    every { contentSynchronizerMock.synchronizeWithRemote(any()) } returns Unit

    every { attributesMock.charset = charsetMock } returns Unit

    mockkStatic(::updateFileTag)
    every { updateFileTag(attributesMock) } returns Unit

    beforeEach {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }
      }
      every { contentSynchronizerMock.isFileSyncPossible(any()) } returns true

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      expectedExitCode = 0
      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }
    }

    val createActionsRef = ChangeEncodingDialog::class.java.getDeclaredMethod("createActions")
    val messageRef = ChangeEncodingDialog::class.java.getDeclaredField("message")
    messageRef.trySetAccessible()

    // ChangeEncodingDialog.createActions
    should("create actions when conversion is possible") {
      val actions = createActionsRef.invoke(changeEncodingDialog) as Array<*>

      val expectedTitle = message("encoding.reload.or.convert.dialog.title", fileName, charsetName)
      val expectedMessage = message("encoding.reload.or.convert.dialog.message", fileName, charsetName)

      assertSoftly { changeEncodingDialog.title shouldBe expectedTitle }
      assertSoftly { messageRef.get(changeEncodingDialog) shouldBe expectedMessage }
      assertSoftly { actions.size shouldBe 3 }
    }
    should("create actions when conversion is not possible") {
      every { contentSynchronizerMock.isFileSyncPossible(any()) } returns false

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      val actions = createActionsRef.invoke(changeEncodingDialog) as Array<*>

      val expectedTitle = message("encoding.reload.dialog.title", fileName, charsetName)
      val expectedMessage = message("encoding.reload.dialog.message", fileName, charsetName)

      assertSoftly { changeEncodingDialog.title shouldBe expectedTitle }
      assertSoftly { messageRef.get(changeEncodingDialog) shouldBe expectedMessage }
      assertSoftly { actions.size shouldBe 2 }
    }
    // reloadAction
    should("run reload action when sync is needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock) } returns Unit

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      // TODO: change it.getValue(Action.NAME) to it.getName() in v1.*.*-231 and greater
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE }
    }
    should("run reload action when sync is not needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock) } returns Unit

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      // TODO: change it.getValue(Action.NAME) to it.getName() in v1.*.*-231 and greater
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE }
    }
    should("run reload action when content synchronizer is null") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
          return null
        }
      }

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock) } returns Unit

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      // TODO: change it.getValue(Action.NAME) to it.getName() in v1.*.*-231 and greater
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE }
    }
    // convertAction
    should("run convert action") {
      mockkStatic(::saveIn)
      every { saveIn(any(), virtualFileMock, charsetMock) } returns Unit

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      // TODO: change it.getValue(Action.NAME) to it.getName() in v1.*.*-231 and greater
      val convertAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.convert") }
      convertAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.CONVERT_EXIT_CODE }
    }

    unmockkAll()
  }
})