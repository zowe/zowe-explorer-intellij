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
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlin.reflect.KFunction

class ExplorerTreeViewTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("Explorer module: ui/ExplorerTreeView") {

    lateinit var fileExplorerView: ExplorerTreeView<*, *, *>
    lateinit var attributesServiceMock: AttributesService<RemoteUssAttributes, MFVirtualFile>

    val explorerMock = mockk<Explorer<ConnectionConfig, FilesWorkingSet>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()

    val openFilesMock = arrayOf<VirtualFile>(mockk(), mockk())
    var closedFileSize = 0

    val dataOpsManagerService = DataOpsManager.getService() as TestDataOpsManagerImpl

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

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }

        override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
          return mockk()
        }

        override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
          attributesClass: Class<out A>,
          vFileClass: Class<out F>
        ): AttributesService<A, F> {
          return if (attributesClass == RemoteUssAttributes::class.java && vFileClass == MFVirtualFile::class.java) {
            attributesServiceMock as AttributesService<A, F>
          } else {
            super.getAttributesService(attributesClass, vFileClass)
          }
        }
      }

      attributesServiceMock = mockk()
      every {
        attributesServiceMock.updateAttributes(any<RemoteUssAttributes>(), any<RemoteUssAttributes>())
      } returns Unit
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
    // updateAttributesForChildrenInEditor
    should("update attributes for files in editor if renamed file is their ancestor") {
      var numOfCalls = 0

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }

        override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
          numOfCalls++
          return if (numOfCalls == 1) {
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

        override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
          attributesClass: Class<out A>,
          vFileClass: Class<out F>
        ): AttributesService<A, F> {
          return if (attributesClass == RemoteUssAttributes::class.java && vFileClass == MFVirtualFile::class.java) {
            attributesServiceMock as AttributesService<A, F>
          } else {
            super.getAttributesService(attributesClass, vFileClass)
          }
        }
      }

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

      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
          return contentSynchronizerMock
        }

        override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
          numOfCalls++
          return if (numOfCalls == 1) mockk<RemoteUssAttributes>() else mockk<FileAttributes>()
        }

        override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
          attributesClass: Class<out A>,
          vFileClass: Class<out F>
        ): AttributesService<A, F> {
          return if (attributesClass == RemoteUssAttributes::class.java && vFileClass == MFVirtualFile::class.java) {
            attributesServiceMock as AttributesService<A, F>
          } else {
            super.getAttributesService(attributesClass, vFileClass)
          }
        }
      }

      fileExplorerView.updateAttributesForChildrenInEditor(mockk<MFVirtualFile>(), "newDir")

      verify(exactly = 0) {
        attributesServiceMock.updateAttributes(any<RemoteUssAttributes>(), any<RemoteUssAttributes>())
      }
    }
  }
})
