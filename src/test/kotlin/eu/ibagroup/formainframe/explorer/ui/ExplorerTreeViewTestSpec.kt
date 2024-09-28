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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.vfs.MFVirtualFile
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
