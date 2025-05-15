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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.AttributesService
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.ContentSynchronizer
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import kotlin.reflect.KFunction

class ExplorerTreeViewTestSpec : AppInitShouldSpec("explorer/ui/ExplorerTreeView", {
  context("all functions") {
    lateinit var fileExplorerView: ExplorerTreeView<*, *, *>
    lateinit var attributesServiceMock: AttributesService<RemoteUssAttributes, MFVirtualFile>

    val explorerMock = mockk<Explorer<ConnectionConfig, FilesWorkingSet>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    val openFilesMock = arrayOf<VirtualFile>(mockk(), mockk())
    var closedFileSize = 0

    val dataOpsManagerService = DataOpsManager.getService()
    every { dataOpsManagerService.componentManager } returns ApplicationManager.getApplication()

    val contentSynchronizerMock = mockk<ContentSynchronizer>()
    every { contentSynchronizerMock.markAsNotNeededForSync(any()) } returns Unit

    val fileEditorManager = mockk<FileEditorManager>()

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
      every { fileEditorManager.openFiles } returns openFilesMock
      every { fileEditorManager.closeFile(any<VirtualFile>()) } answers {
        closedFileSize++
      }

      mockkStatic(FileEditorManager::getInstance)
      every { FileEditorManager.getInstance(any()) } returns fileEditorManager

      val isAncestorRef: (VirtualFile, VirtualFile, Boolean) -> Boolean = VfsUtilCore::isAncestor
      mockkStatic(isAncestorRef as KFunction<*>)
      every { VfsUtilCore.isAncestor(any<VirtualFile>(), any<VirtualFile>(), any<Boolean>()) } returns true

      attributesServiceMock = mockk {
        every { updateAttributes(any<RemoteUssAttributes>(), any<RemoteUssAttributes>()) } returns Unit
      }

      every { dataOpsManagerService.getContentSynchronizer(any<VirtualFile>()) } returns contentSynchronizerMock
      every { dataOpsManagerService.tryToGetAttributes(any<VirtualFile>()) } returns mockk()
      every {
        dataOpsManagerService.getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
      } returns attributesServiceMock
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
    // updateAttributesForChildrenInEditor
    should("update attributes for files in editor if renamed file is their ancestor") {
      var numOfCalls = 0

      every {
        dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
      } answers {
        numOfCalls++
        if (numOfCalls == 1) {
          mockk<RemoteUssAttributes> {
            every { path } returns "/u/USER/dir/"
            every { parentDirPath } returns "/u/USER"
          }
        } else {
          RemoteUssAttributes(
            "/u/USER/dir/file.txt",
            false,
            mockk(),
            "https://hostname:port",
            mutableListOf()
          )
        }
      }
      every {
        dataOpsManagerService.getAttributesService(RemoteUssAttributes::class.java, MFVirtualFile::class.java)
      } returns attributesServiceMock

      fileExplorerView.updateAttributesForChildrenInEditor(mockk<MFVirtualFile>(), "newDir")

      verify { attributesServiceMock.updateAttributes(any<RemoteUssAttributes>(), any<RemoteUssAttributes>()) }
    }
    should("don't update attributes for files in editor if renamed file is not their ancestor") {
      every { VfsUtilCore.isAncestor(any<VirtualFile>(), any<VirtualFile>(), any<Boolean>()) } returns false

      fileExplorerView.updateAttributesForChildrenInEditor(mockk<MFVirtualFile>(), "newDir")

      verify(exactly = 0) {
        attributesServiceMock.updateAttributes(any<RemoteUssAttributes>(), any<RemoteUssAttributes>())
      }
    }
    should("don't update attributes for files in editor if attributes are not USS attributes") {
      fileExplorerView.updateAttributesForChildrenInEditor(mockk<MFVirtualFile>(), "newDir")

      verify(exactly = 0) {
        attributesServiceMock.updateAttributes(any<RemoteUssAttributes>(), any<RemoteUssAttributes>())
      }
    }
    should("don't update attributes for files in editor if old attributes are not USS attributes") {
      var numOfCalls = 0

      every {
        dataOpsManagerService.tryToGetAttributes(any<VirtualFile>())
      } answers {
        numOfCalls++
        if (numOfCalls == 1) mockk<RemoteUssAttributes>() else mockk<FileAttributes>()
      }

      fileExplorerView.updateAttributesForChildrenInEditor(mockk<MFVirtualFile>(), "newDir")

      verify(exactly = 0) {
        attributesServiceMock.updateAttributes(any<RemoteUssAttributes>(), any<RemoteUssAttributes>())
      }
    }
  }
})
