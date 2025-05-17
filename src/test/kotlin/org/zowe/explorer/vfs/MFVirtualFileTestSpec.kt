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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.vfs

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.*
import org.zowe.explorer.testutils.MockkAwareShouldSpec

class MFVirtualFileTestSpec : MockkAwareShouldSpec({
  context("vfs/MFVirtualFile") {
    val fsModelMock = mockk<MFVirtualFileSystemModel>()
    val fsMock = mockk<MFVirtualFileSystem> {
      every { model } returns fsModelMock
    }
    mockkObject(MFVirtualFile.Companion, recordPrivateCalls = true)
    every { MFVirtualFile getProperty "fs" } returns fsMock

    mockkStatic(VirtualFileManager::getInstance)
    every { VirtualFileManager.getInstance() } returns mockk<VirtualFileManager> {
      every { getFileSystem(MFVirtualFileSystem.PROTOCOL) } returns fsMock
    }

    mockkStatic(FileTypeRegistry::getInstance)

    val initAttributesMock = mockk<FileAttributes> {
      every { isWritable } returns true
      every { isDirectory } returns false
    }

    beforeEach {
      every { fsModelMock.getParent(any<MFVirtualFile>()) } returns mockk()
      every { fsModelMock.isFileValid(any<MFVirtualFile>()) } returns true
      every { fsModelMock.resolveAndGetSymlink(any<MFVirtualFile>()) } returns mockk()
      every { fsModelMock.getChildren(any<MFVirtualFile>()) } returns emptyArray()

      every { fsMock.isSymLink(any<MFVirtualFile>()) } returns false

      every { FileTypeRegistry.getInstance() } returns mockk {
        every { getFileTypeByFile(any<MFVirtualFile>()) } returns FileTypes.UNKNOWN
      }
    }

    context("getPath") {
      should("get file path when the file is valid") {
        every { fsModelMock.getParent(any<MFVirtualFile>()) } returns mockk {
          every { path } returns "/test_parent_path/"
        }
        val virtualFile = MFVirtualFile(1, "test_file_name", initAttributesMock)
        val result = virtualFile.path
        assertSoftly { result shouldBe "/test_parent_path/test_file_name/" }
      }
      should("get file path when the file is valid and the parent is null") {
        every { fsModelMock.getParent(any<MFVirtualFile>()) } returns null
        val virtualFile = MFVirtualFile(1, "test_file_name", initAttributesMock)
        val result = virtualFile.path
        assertSoftly { result shouldBe "test_file_name/" }
      }
      should("get old file path when the file is not valid") {
        every { fsModelMock.isFileValid(any<MFVirtualFile>()) } returns false
        val virtualFile = MFVirtualFile(1, "test_file_name", initAttributesMock)
        virtualFile.intermediateOldPathInternal = "/test_old_path"
        val result = virtualFile.path
        assertSoftly { result shouldBe "/test_old_path" }
      }
    }
    context("getCanonicalFile") {
      should("get the real file instance by the symlink") {
        every { fsMock.isSymLink(any<MFVirtualFile>()) } returns true
        val virtualFile = MFVirtualFile(1, "test", initAttributesMock)
        val result = virtualFile.canonicalFile
        assertSoftly { result shouldNotBeSameInstanceAs virtualFile }
      }
      should("just return the original virtual file when it is not a symlink") {
        val virtualFile = MFVirtualFile(1, "test", initAttributesMock)
        val result = virtualFile.canonicalFile
        assertSoftly { result shouldBeSameInstanceAs virtualFile }
      }
    }
    context("getChildren") {
      should("get children if the virtual file is a directory") {
        every { fsModelMock.getChildren(any<MFVirtualFile>()) } returns arrayOf(mockk())
        val locInitAttributesMock = mockk<FileAttributes> {
          every { isWritable } returns true
          every { isDirectory } returns true
        }
        val virtualFile = MFVirtualFile(1, "test", locInitAttributesMock)
        val result = virtualFile.children
        assertSoftly { result shouldNotBe null }
        assertSoftly { result?.size shouldBe 1 }
      }
      should("return null if the virtual file is not a directory") {
        val virtualFile = MFVirtualFile(1, "test", initAttributesMock)
        val result = virtualFile.children
        assertSoftly { result shouldBe null }
      }
    }
    context("findChild") {
      should("find a child in the directory by it's name") {
        val childNameToFind = "test_to_find"
        val mockVFileToFind = MFVirtualFile(2, childNameToFind, initAttributesMock)
        every {
          fsModelMock.getChildren(any<MFVirtualFile>())
        } returns arrayOf(
          mockVFileToFind,
          MFVirtualFile(3, "test_to_skip", initAttributesMock)
        )
        val locInitAttributes = mockk<FileAttributes> {
          every { isDirectory } returns true
          every { isWritable } returns true
        }
        val virtualFile = MFVirtualFile(1, "test_dir", locInitAttributes)
        val result = virtualFile.findChild(childNameToFind)

        assertSoftly { result shouldBeSameInstanceAs mockVFileToFind }
      }
      should("return null instead of a child cause there is not such child in the directory") {
        every {
          fsModelMock.getChildren(any<MFVirtualFile>())
        } returns arrayOf(MFVirtualFile(3, "test_to_skip", initAttributesMock))
        val locInitAttributes = mockk<FileAttributes> {
          every { isDirectory } returns true
          every { isWritable } returns true
        }
        val virtualFile = MFVirtualFile(1, "test_dir", locInitAttributes)
        val result = virtualFile.findChild("test_to_find")

        assertSoftly { result shouldBe null }
      }
      should("return null instead of a child cause the virtual file is not a directory") {
        val virtualFile = MFVirtualFile(1, "test_dir", initAttributesMock)
        val result = virtualFile.findChild("test_to_find")

        assertSoftly { result shouldBe null }
      }
    }
    context("getFileType") {
      should("get a file type of the virtual file instance") {
        every { FileTypeRegistry.getInstance() } returns mockk {
          every { getFileTypeByFile(any<MFVirtualFile>()) } returns FileTypes.ARCHIVE
        }
        val virtualFile = MFVirtualFile(1, "test", initAttributesMock)
        val result = virtualFile.fileType

        assertSoftly { result shouldBe FileTypes.ARCHIVE }
      }
      should("return plain text file type if the defined file type is unknown") {
        val virtualFile = MFVirtualFile(1, "test", initAttributesMock)
        val result = virtualFile.fileType

        assertSoftly { result shouldBe PlainTextFileType.INSTANCE }
      }
    }
    context("equals") {
      should("return true if two virtual files are equal by the file ID") {
        val virtualFile1 = MFVirtualFile(1, "test", initAttributesMock)
        val virtualFile2 = MFVirtualFile(1, "test", initAttributesMock)
        val result = virtualFile1 == virtualFile2
        assertSoftly { result shouldBe true }
      }
      should("return false if two virtual files are not equal by the file ID") {
        val virtualFile1 = MFVirtualFile(1, "test", initAttributesMock)
        val virtualFile2 = MFVirtualFile(2, "test", initAttributesMock)
        val result = virtualFile1 == virtualFile2
        assertSoftly { result shouldBe false }
      }
    }
  }
})
