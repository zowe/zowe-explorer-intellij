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

package org.zowe.explorer.editor

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.isComponentUnderMouse
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.content.service.SyncProcessService
import org.zowe.explorer.dataops.content.synchronizer.AutoSyncFileListener
import org.zowe.explorer.dataops.content.synchronizer.ContentSynchronizer
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.checkEncodingCompatibility
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.utils.showSaveAnywayDialog
import org.zowe.explorer.vfs.MFVirtualFile
import java.awt.Component
import java.awt.Point
import java.awt.event.FocusEvent
import java.nio.charset.Charset
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.reflect.KFunction

class FileEditorFocusListenerTestSpec : AppInitShouldSpec("editor/FileEditorFocusListener", {
  context("editor module: FileEditorFocusListener") {
    val fileEditorFocusListener = FileEditorFocusListener()

    val editorComponentMock = mockk<JComponent>()
    val editorMock = mockk<EditorEx> {
      every { component } returns editorComponentMock
    }

    mockkStatic(Component::isComponentUnderMouse)

    val point = Point(0, 0)
    val oppositeComponentMock = mockk<Component> {
      every { locationOnScreen } returns point
    }
    val focusEventMock = mockk<FocusEvent> {
      every { oppositeComponent } returns oppositeComponentMock
    }

    mockkStatic(SwingUtilities::convertPointFromScreen)
    every { SwingUtilities.convertPointFromScreen(point, editorComponentMock) } returns Unit

    val projectMock = mockk<Project>()
    val charsetMock = mockk<Charset>()
    val virtualFileMock = mockk<MFVirtualFile> {
      every { name } returns "fileName"
      every { charset } returns charsetMock
    }

    val bytes = byteArrayOf(116, 101, 120, 116)

    val contentSynchronizerMock = mockk<ContentSynchronizer> {
      every { successfulContentStorage(any()) } returns bytes
    }

    var currentBytes: ByteArray

    mockkConstructor(DocumentedSyncProvider::class)
    every { anyConstructed<DocumentedSyncProvider>().saveDocument() } returns Unit

    mockkStatic(::checkEncodingCompatibility)

    mockkStatic(::showSaveAnywayDialog)

    var isSynced = false
    val sendTopicRef: (Topic<AutoSyncFileListener>, Project) -> AutoSyncFileListener = ::sendTopic
    mockkStatic(sendTopicRef as KFunction<*>)
    every { sendTopic(AutoSyncFileListener.AUTO_SYNC_FILE, any<Project>()) } answers {
      isSynced = true
      val autoSyncFileListenerMock = mockk<AutoSyncFileListener>()
      every { autoSyncFileListenerMock.sync(virtualFileMock) } returns Unit
      autoSyncFileListenerMock
    }

    val configService = ConfigService.getService()
    val dataOpsManagerService = DataOpsManager.getService()
    val syncProcessService = SyncProcessService.getService()

    beforeEach {
      every { editorComponentMock.isComponentUnderMouse() } returns false
      every { editorComponentMock.contains(any()) } returns false

      every { configService.isAutoSyncEnabled } returns true

      every { editorMock.project } returns projectMock
      every { editorMock.virtualFile } returns virtualFileMock

      every { virtualFileMock.isWritable } returns true

      every { dataOpsManagerService.getContentSynchronizer(any<VirtualFile>()) } returns contentSynchronizerMock

      every { syncProcessService.isFileSyncingNow(any<VirtualFile>()) } returns false

      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns true

      currentBytes = bytes.clone()
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
      every { configService.isAutoSyncEnabled } returns true

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
      every { dataOpsManagerService.getContentSynchronizer(any<VirtualFile>()) } returns null

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when file upload is not needed") {
      every { contentSynchronizerMock.isFileUploadNeeded(any()) } returns false

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
    should("not perform auto file sync when file is syncing now") {
      currentBytes = byteArrayOf(116, 101, 120, 116, 33)

      every { syncProcessService.isFileSyncingNow(any<VirtualFile>()) } returns true

      fileEditorFocusListener.focusLost(editorMock, focusEventMock)

      assertSoftly { isSynced shouldBe false }
    }
  }
})
