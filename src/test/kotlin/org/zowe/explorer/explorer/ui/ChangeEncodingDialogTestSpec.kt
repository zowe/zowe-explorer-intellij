/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.zowe.explorer.common.message
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.ContentSynchronizer
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.*
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.awt.event.ActionEvent
import java.nio.charset.Charset
import javax.swing.Action
import javax.swing.Icon
import kotlin.reflect.full.declaredFunctions

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
    every { virtualFileMock.charset } returns charsetMock
    every { charsetMock.name() } returns charsetName
    every { charsetMock.displayName() } returns charsetName

    var safeToReload = Magic8.ABSOLUTELY
    var safeToConvert = Magic8.ABSOLUTELY

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

    val showDialogRef = Messages::class.declaredFunctions
      .first { it.name == "showDialog" && it.parameters.size == 5 }
    mockkStatic(showDialogRef)

    every { contentSynchronizerMock.synchronizeWithRemote(any()) } returns Unit

    every { attributesMock.charset = charsetMock } returns Unit

    mockkStatic(::updateFileTag)
    every { updateFileTag(attributesMock) } returns Unit

    beforeEach {
      safeToReload = Magic8.ABSOLUTELY
      safeToConvert = Magic8.ABSOLUTELY

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }
      }
      every { attributesMock.isWritable } returns true

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 1

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
      every { attributesMock.isWritable } returns false

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
    should("create actions when encoding is incompatible") {
      safeToReload = Magic8.NO_WAY
      safeToConvert = Magic8.NO_WAY

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      val icon1 = actions?.get(0)?.getValue(Action.SMALL_ICON)
      val icon2 = actions?.get(1)?.getValue(Action.SMALL_ICON)

      assertSoftly { icon1 shouldBe AllIcons.General.Warning }
      assertSoftly { icon2 shouldBe AllIcons.General.Warning }
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
    should("run reload action when encoding is incompatible but user reloads anyway") {
      safeToReload = Magic8.NO_WAY

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 0

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
    should("not run reload action when encoding is incompatible") {
      safeToReload = Magic8.NO_WAY

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      // TODO: change it.getValue(Action.NAME) to it.getName() in v1.*.*-231 and greater
      val reloadAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.reload") }
      reloadAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe DialogWrapper.CANCEL_EXIT_CODE }
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
    should("run convert action when encoding is incompatible but user converts anyway") {
      safeToConvert = Magic8.NO_WAY

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      every {
        Messages.showDialog(any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any<Icon>())
      } returns 0

      mockkStatic(::saveIn)
      every { saveIn(any(), virtualFileMock, charsetMock) } returns Unit

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      // TODO: change it.getValue(Action.NAME) to it.getName() in v1.*.*-231 and greater
      val convertAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.convert") }
      convertAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe ChangeEncodingDialog.CONVERT_EXIT_CODE }
    }
    should("not run convert action when encoding is incompatible") {
      safeToConvert = Magic8.NO_WAY

      changeEncodingDialog = spyk(
        ChangeEncodingDialog(
          virtualFileMock,
          attributesMock,
          charsetMock,
          safeToReload,
          safeToConvert
        )
      )

      every { changeEncodingDialog["close"](any<Int>()) } answers {
        expectedExitCode = firstArg<Int>()
        this
      }

      val actions = createActionsRef.invoke(changeEncodingDialog).castOrNull<Array<Action>>()
      // TODO: change it.getValue(Action.NAME) to it.getName() in v1.*.*-231 and greater
      val convertAction = actions?.first { it.getValue(Action.NAME) == IdeBundle.message("button.convert") }
      convertAction?.actionPerformed(actionEventMock)

      assertSoftly { expectedExitCode shouldBe DialogWrapper.CANCEL_EXIT_CODE }
    }

    unmockkAll()
  }
})
