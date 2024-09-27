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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestConfigServiceImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.reloadIn
import eu.ibagroup.formainframe.utils.saveIn
import eu.ibagroup.formainframe.utils.updateFileTag
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.event.ActionEvent
import java.nio.charset.Charset
import javax.swing.Action
import javax.swing.Icon
import kotlin.reflect.KFunction

class ChangeEncodingDialogTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("Explorer module: ui/ChangeEncodingDialog") {

    lateinit var changeEncodingDialog: ChangeEncodingDialog
    var expectedExitCode = 0
    var isSynced = false

    val projectMock = mockk<Project>()

    val fileName = "fileName"
    val virtualFileMock = mockk<VirtualFile>()
    every { virtualFileMock.name } returns fileName

    val attributesMock = mockk<RemoteUssAttributes>()

    val charsetName = "charsetName"
    val charsetMock = mockk<Charset>()
    every { virtualFileMock.charset } returns charsetMock
    every { charsetMock.name() } returns charsetName
    every { charsetMock.displayName() } returns charsetName

    var safeToReload = Magic8.ABSOLUTELY
    var safeToConvert = Magic8.ABSOLUTELY

    val contentSynchronizerMock = mockk<ContentSynchronizer>()
    val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl
    val configService = ConfigService.getService() as TestConfigServiceImpl

    mockkConstructor(DocumentedSyncProvider::class)
    every { anyConstructed<DocumentedSyncProvider>().saveDocument() } returns Unit

    val documentMockk = mockk<Document>()
    every { anyConstructed<DocumentedSyncProvider>().getDocument() } returns documentMockk

    mockkObject(ChangeEncodingDialog)
    every { ChangeEncodingDialog["initialize"](any<() -> Unit>()) } returns Unit

    val actionEventMock = mockk<ActionEvent>()

    val showDialogRef: (String, String, Array<String>, Int, Icon) -> Int = Messages::showDialog
    mockkStatic(showDialogRef as KFunction<*>)

    every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } answers {
      isSynced = true
    }

    every { attributesMock.charset = charsetMock } returns Unit

    mockkStatic(::updateFileTag)
    every { updateFileTag(attributesMock) } returns Unit

    val projectManagerMock = mockk<ProjectManager>()

    mockkStatic(ProjectManager::getInstance)
    every { ProjectManager.getInstance() } returns projectManagerMock

    mockkObject(MessageDialogBuilder.Companion)

    beforeEach {
      safeToReload = Magic8.ABSOLUTELY
      safeToConvert = Magic8.ABSOLUTELY

      isSynced = false

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }
      }
      every { attributesMock.isWritable } returns true

      every { projectManagerMock.openProjects } returns arrayOf(mockk())

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 1

      expectedExitCode = 0
      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false
      configService.isAutoSyncEnabled = true

      every { MessageDialogBuilder.yesNo(any<String>(), any<String>()) } returns mockk {
        every { asWarning() } returns this
        every { ask(any<Project>()) } returns true
      }
    }

    val createActionsRef = ChangeEncodingDialog::class.java.getDeclaredMethod("createActions")
    val messageRef = ChangeEncodingDialog::class.java.getDeclaredField("message")
    messageRef.trySetAccessible()

    // ChangeEncodingDialog.createActions
    should("create actions when conversion is possible") {
      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog) as Array<*>
        }
      }

      val expectedTitle = message("encoding.reload.or.convert.dialog.title", fileName, charsetName)
      val expectedMessage = message("encoding.reload.or.convert.dialog.message", fileName, charsetName)

      assertSoftly { changeEncodingDialog.title shouldBe expectedTitle }
      assertSoftly { messageRef.get(changeEncodingDialog) shouldBe expectedMessage }
      assertSoftly { actions.size shouldBe 3 }
    }
    should("create actions when conversion is not possible") {
      every { attributesMock.isWritable } returns false

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog) as Array<*>
        }
      }

      val expectedTitle = message("encoding.reload.dialog.title", fileName, charsetName)
      val expectedMessage = message("encoding.reload.dialog.message", fileName, charsetName)

      assertSoftly { changeEncodingDialog.title shouldBe expectedTitle }
      assertSoftly { messageRef.get(changeEncodingDialog) shouldBe expectedMessage }
      assertSoftly { actions.size shouldBe 2 }
    }
    should("create actions when conversion is disabled") {
      every { projectManagerMock.openProjects } returns arrayOf(mockk(), mockk())

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val actualConvertAction = actions?.get(1)

      val expectedTooltip = message("encoding.convert.button.error.tooltip")

      assertSoftly {
        actualConvertAction?.isEnabled shouldBe false
        actualConvertAction?.getValue(Action.SHORT_DESCRIPTION) shouldBe expectedTooltip
        actions?.size shouldBe 3
      }
    }
    should("create actions when encoding is incompatible") {
      safeToReload = Magic8.NO_WAY
      safeToConvert = Magic8.NO_WAY

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val icon1 = actions?.get(0)?.getValue(Action.SMALL_ICON)
      val icon2 = actions?.get(1)?.getValue(Action.SMALL_ICON)

      assertSoftly { icon1 shouldBe AllIcons.General.Warning }
      assertSoftly { icon2 shouldBe AllIcons.General.Warning }
    }
    // reloadAction
    should("run reload action when sync is needed and auto sync is enabled") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock, any()) } returns Unit

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly {
        isSynced shouldBe true
        expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE
      }
    }
    should("run reload action when sync is needed and user wants to sync it") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock, any()) } returns Unit

      configService.isAutoSyncEnabled = false

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly {
        isSynced shouldBe true
        expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE
      }
    }
    should("run reload action when sync is needed but user doesn't want to sync it") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock, any()) } returns Unit

      configService.isAutoSyncEnabled = false
      every { MessageDialogBuilder.yesNo(any<String>(), any<String>()) } returns mockk {
        every { asWarning() } returns this
        every { ask(any<Project>()) } returns false
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly {
        isSynced shouldBe false
        expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE
      }
    }
    should("run reload action when sync is not needed") {
      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock, any()) } returns Unit

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly {
        isSynced shouldBe false
        expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE
      }
    }
    should("run reload action when content synchronizer is null") {
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
          return null
        }
      }

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock, any()) } returns Unit

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE }
    }
    should("run reload action when encoding is incompatible but user reloads anyway") {
      safeToReload = Magic8.NO_WAY

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 0

      mockkStatic(::reloadIn)
      every { reloadIn(any(), virtualFileMock, charsetMock, any()) } returns Unit

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.RELOAD_EXIT_CODE }
    }
    should("not run reload action when encoding is incompatible") {
      safeToReload = Magic8.NO_WAY

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe DialogWrapper.CANCEL_EXIT_CODE }
    }
    // convertAction
    should("run convert action") {
      mockkStatic(::saveIn)
      every { saveIn(any(), virtualFileMock, charsetMock) } returns Unit

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val convertAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.convert") }
      convertAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.CONVERT_EXIT_CODE }
    }
    should("run convert action when encoding is incompatible but user converts anyway") {
      safeToConvert = Magic8.NO_WAY

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 0

      mockkStatic(::saveIn)
      every { saveIn(any(), virtualFileMock, charsetMock) } returns Unit

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val convertAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.convert") }
      convertAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.CONVERT_EXIT_CODE }
    }
    should("not run convert action when encoding is incompatible") {
      safeToConvert = Magic8.NO_WAY

      changeEncodingDialog = runBlocking {
        withContext(Dispatchers.EDT) {
          spyk(
            ChangeEncodingDialog(
              projectMock,
              virtualFileMock,
              attributesMock,
              charsetMock,
              safeToReload,
              safeToConvert
            )
          )
        }
      }

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = runBlocking {
        withContext(Dispatchers.EDT) {
          createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
        }
      }
      val convertAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.convert") }
      convertAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe DialogWrapper.CANCEL_EXIT_CODE }
    }

    unmockkAll()
  }
})
