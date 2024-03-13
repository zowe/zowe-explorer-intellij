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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.explorer.*
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.service
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlin.reflect.KFunction

class ExplorerTreeViewTestSpec: WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("Explorer module: ui/ExplorerTreeView") {

    lateinit var fileExplorerView: ExplorerTreeView<*, *, *>

    val explorerMock = mockk<Explorer<ConnectionConfig, FilesWorkingSet>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    val openFilesMock = arrayOf<VirtualFile>(mockk(), mockk())
    var closedFileSize = 0

    val dataOpsManagerService =
      ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl

    val contentSynchronizerMock = mockk<ContentSynchronizer>()
    every { contentSynchronizerMock.markAsNotNeededForSync(any()) } returns Unit

    beforeEach {
      mockkConstructor(CommonExplorerTreeStructure::class)
      every { anyConstructed<CommonExplorerTreeStructure<*>>().rootElement } returns Unit

      fileExplorerView = spyk(
        FileExplorerView(
          explorerMock,
          mockk(),
          mockk(),
          mockk(),
          { _, _, _ -> mockk() }
        ) { }
      )

      closedFileSize = 0
      mockkStatic(FileEditorManager::getInstance)
      every { FileEditorManager.getInstance(any()) } returns object : TestFileEditorManager() {
        override fun getOpenFiles(): Array<VirtualFile> {
          return openFilesMock
        }

        override fun closeFile(file: VirtualFile) {
          closedFileSize++
        }
      }

      val isAncestorRef: (VirtualFile, VirtualFile, Boolean) -> Boolean = VfsUtilCore::isAncestor
      mockkStatic(isAncestorRef as KFunction<*>)
      every { VfsUtilCore.isAncestor(any<VirtualFile>(), any<VirtualFile>(), any<Boolean>()) } returns true

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }
      }
    }

    afterEach {
      unmockkAll()
    }

    // closeChildrenInEditor
    should("close files in editor if selected file is their ancestor") {
      fileExplorerView.closeChildrenInEditor(mockk())

      assertSoftly { closedFileSize shouldBe openFilesMock.size }
    }
    should("don't close files in editor if selected file is not their ancestor") {
      every { VfsUtilCore.isAncestor(any<VirtualFile>(), any<VirtualFile>(), any<Boolean>()) } returns false

      fileExplorerView.closeChildrenInEditor(mockk())

      assertSoftly { closedFileSize shouldBe 0 }
    }
  }
})